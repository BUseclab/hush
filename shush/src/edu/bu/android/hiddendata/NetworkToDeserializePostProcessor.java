package edu.bu.android.hiddendata;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.bu.android.hiddendata.PostProcessor.ListFlow;
import edu.bu.android.hiddendata.ModelExtraction.OnExtractionHandler;
import edu.bu.android.hiddendata.model.DeserializeToUIConfig;
import edu.bu.android.hiddendata.model.InjectionPoint;
import edu.bu.android.hiddendata.model.JsonUtils;
import edu.bu.android.hiddendata.model.Model;
import soot.Body;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultInfo;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.util.Chain;

/**
 * Process the results of the flow analysis from network connections to JSON deserialization.
 * 
 * Find the model object and write all get methods to a new sink file to run as a second pass.
 * 
 * @author Wil Koch
 *
 */
public class NetworkToDeserializePostProcessor extends PostProcessor {

	//private final Hashtable<String, ObjectExtractionQuery> extractionPoints;
	//private final Hashtable<String,Integer> modelParameterIndexLookup = new Hashtable<String, Integer>();
	private static final Logger logger = LoggerFactory.getLogger(NetworkToDeserializePostProcessor.class.getName());

	private InfoflowResults results;
	private final String apkFileName;
	private File sourcesAndSinksFile;
	private File easyTaintWrapperFile;
	private File resultsFile;
	/**
	 * What model is mapped to the signature so it can be created for injection
	 */
	private HashMap<String, String> signatureToModelMapping = new HashMap<String, String>();

	private File sourcesAndSinksListFile;
	
	
	public NetworkToDeserializePostProcessor(SetupApplication context, File sourcesAndSinksFile, File sourceAndSinkListFile, File easyTaintFile, File config, InfoflowResults results){
		super(context);
		this.apkFileName = new File(context.getApkFileLocation()).getName();
		this.results = results;
		this.sourcesAndSinksFile = sourcesAndSinksFile;
		this.easyTaintWrapperFile = easyTaintFile;
		this.resultsFile = config;//new File(sourcesAndSinksFile.getParentFile(),apkFileName +  FindHidden.MODEL_TO_UI_CONFIG_SUFFIX );
		
		this.sourcesAndSinksListFile = sourceAndSinkListFile;
		
		//TODO stick in config file
		/*
		modelParameterIndexLookup.put("<com.google.gson.Gson: java.lang.Object fromJson(java.lang.String,java.lang.Class)>", 1);
		modelParameterIndexLookup.put("<com.google.gson.Gson: java.lang.Object fromJson(com.google.gson.JsonElement,java.lang.Class)>", 1);
		modelParameterIndexLookup.put("<com.google.gson.Gson: java.lang.Object fromJson(java.io.Reader,java.lang.Class)>", 1);
		modelParameterIndexLookup.put("<java.util.List: boolean add(java.lang.Object)>", 0);
		*/
	}
	
	
	/**
	 * Process the results of the flow to extract the models from the json deserilize method
	 */
	public boolean process(){
		boolean processLists = false;
		if (results == null){
			return processLists;
		}
		
		HashMap<String, String> deserializeToModelMapping = new HashMap<String, String>();
		//Track all the objects that are added to lists
		HashMap<String, ListFlow> addMethodParameterClassNames = new HashMap<String, ListFlow>();
		Set<String> modelClassNames = new HashSet<String>();

		//Keep track of original sources
		//so we can use this for the model-ui pass
		//to determine if we can use the source 
		Set<String> originalSinks = new HashSet<String>();

		Set<String> sourceSignatures = new HashSet<String>();
		
		for (ResultSinkInfo sink : results.getResults().keySet()) {
			Stmt sinkStmt = sink.getSink();
			
			boolean isDeserializeSink = false;
			//Look at the sources and see which are actually specified in our source sink file
			//so we can filter out possible onces found from the callbacks enabled
			for (ResultSourceInfo source : results.getResults().get(sink)) {
				
				//Make sure its form original source
				if (isOriginalSource(source.getSource().toString())){
					
					/*
					if (isListFlow(sink, source)){
						ListFlow listFlow = new ListFlow();
						listFlow.source = source;
						listFlow.sink = sink;
						String addClassName = extractClassFromListAdd(sink.getSink());
						addMethodParameterClassNames.put(addClassName, listFlow);
						//isDeserializeSink = false;
					} else {
						isDeserializeSink = true;
					}
					 */
					isDeserializeSink = true;

				}
			}
			
			if (isDeserializeSink){
					//Get the sink and add to file for next pass. We can use the entire method call
					//because soot will track taint of the casting
					String sig = makeSignature(sink);
					sourceSignatures.add(sig);
					originalSinks.add(sig);
					//Extract the model class from the deserialize method call so we can compare it 
					//to list objects
					String className = extractModelClassName(sink);
					if (className != null){
						deserializeToModelMapping.put(sig, className);
						modelClassNames.add(className);
					}
			}
			
			
			
		}
		
		Set<InjectionPoint> injections = new HashSet<InjectionPoint>();
		Set<String> sinkSignatures = new HashSet<String>();
		
		
		
		List<Model> models = new ArrayList<Model>();
		final Set<String> modelMethodSignatures = new HashSet<String>();

		//Now that we have all the base models, analyze each
		//and get any other models they reference
		ModelExtraction me = new ModelExtraction();
		
		//For optimization since we already need to loop through all methods
		//also save all the get methods so we can on the second pass
		//use them to compare to what we found
		me.addHandler(new OnExtractionHandler() {
			
			@Override
			public void onMethodExtracted(SootClass sootClass, SootMethod method) {
				
				String methodName = method.getName();
				if (!(method.getReturnType() instanceof VoidType) && !methodName.equals("toString") 
						&& !methodName.equals("describeContents")
						&& !methodName.equals("compareTo")) {

				//if (shouldAcceptMethod(methodName)){
					modelMethodSignatures.add(method.getSignature());

				}
				
			}

			@Override
			public void onFieldExtracdted(SootClass sootClass,
					SootField sootField) {
				modelMethodSignatures.add(sootField.getSignature());
			}
		});
		
		
		//Make the default constructors for the model classes found 
		//and also find all other associated models
		Set<String> allModels = new HashSet<String>();
		for (String model : modelClassNames){
			allModels.addAll(me.getModels(model));
		}
		
		//Add model constructors that are to be injected into lists to force taint
		//Moving this to be done in ListAnalyzer
		//for (String model : allModels){
		//	sourceSignatures.add(makeDefaultSignatureConstructor(model));
		//}
		
		//Add all model get methods as sources. This is to workaround issue 
		//with agnostic results not necessarily chosen the path that has 
		//the model fully tainted and therefore during post analysis can not be retrieved
		//to find what get methods are at sink
		sourceSignatures.addAll(modelMethodSignatures);
		
		
		Set<String> listConstructorSources = new HashSet<String>();
		Set<String> listAddModelSignatures = findAddMethods(listConstructorSources, allModels);
		ConfigUtils.createSinkSourceFile("SourcesAndSinks_2.txt", sourcesAndSinksListFile, listConstructorSources, listAddModelSignatures);
		
		//Write out results to be used for next pass
		DeserializeToUIConfig result = new DeserializeToUIConfig();

		result.setOriginalSinks(originalSinks);
		result.setDeserializeToModelMapping(deserializeToModelMapping);
		result.setGetMethodSignatures(modelMethodSignatures);
		result.setModelNames(allModels);
		result.setModelToListSignatureMapping(signatureToModelMapping);
		//result.setInjections(injections); //Done in ListAnalyzer
		JsonUtils.writeResults(resultsFile, result);
		
		ConfigUtils.createEasyTaintWrapperFile(easyTaintWrapperFile,allModels);
		
		//TODO Need to have the model constructuros specific to the injection
		//Now create a new sink source file for the next pass
		ConfigUtils.createSinkSourceFile("./Sinks_ui.txt", sourcesAndSinksFile, sourceSignatures, sinkSignatures);
	
		
		return !listConstructorSources.isEmpty() && !listAddModelSignatures.isEmpty();
	}
	
	
	/**
	 * Create sigatures for all the add methods. We do this so we limit the number of sinks to this method
	 * @param listConstructorSources
	 * @param modelClassNames All known models
	 * @return All add and addAll methods
	 */
	private  Set<String> findAddMethods(final Set<String> listConstructorSources , final Set<String> modelClassNames){
		
		final Set<String> addMethodSignatures = new HashSet<String>();
		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			final SootClass sc = it.next();
			if (sc.toString().contains("com.github.wil3.android.flowtests.D")){
				//sc.setApplicationClass();
				//new ModelExtraction().getModels(sc.getName());
			}
			if (isFrameworkClass(sc.getName())){
				continue;
			}
			boolean foundInMethod = false;
			for (SootMethod method : sc.getMethods()){
				if (!method.hasActiveBody()){
					try {
					method.retrieveActiveBody();
					}catch (Exception e){
						continue;
					}
				}
				final Body body = method.retrieveActiveBody();
				final PatchingChain<Unit> units = body.getUnits();
				Unit[] unitArray = new Unit[units.size()];
				unitArray = units.toArray(unitArray);
				//for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
				//	final Unit u = iter.next();
				for (int i=0; i<unitArray.length; i++){
				//	Stmt stmt = (Stmt)u;
					Stmt stmt = (Stmt)unitArray[i];		
					
					if (stmt instanceof InvokeStmt){
					
						if (stmt.toString().contains("<java.util.List: boolean add(java.lang.Object)>")
								||stmt.toString().contains("<java.util.ArrayList: boolean add(java.lang.Object)>") ){
							
						 	String modelClassName = extractClassFromListAdd(stmt);
						 	if (modelClassName != null && modelClassNames.contains(modelClassName)){
						 		String sig = makeSignature(sc, stmt);
						 		addMethodSignatures.add(sig);
						 		signatureToModelMapping.put(sig, modelClassName);
						 		//If we have one per method thats good for now 
						 		foundInMethod = true;
						 		break;
						 	}
						 	
						} else if (stmt.toString().contains("<java.util.List: boolean addAll(java.util.Collection)>")){
							//A casting must occur right before this
							String baseClass = findCasting(unitArray, i);
							if (baseClass != null && modelClassNames.contains(baseClass)){
								String sig = makeSignature(sc, stmt);
						 		addMethodSignatures.add(sig);
						 		signatureToModelMapping.put(sig, baseClass);
							}
						
						} else if (stmt.toString().contains("<java.util.ArrayList: void <init>()>")){
					 		String sig = makeSignature(sc, stmt);
					 		listConstructorSources.add(sig);
					 	}
					
					} else if (stmt instanceof AssignStmt){
						
						//Dont need this anymore because we found generics in tag signatures!
						/*
						AssignStmt assignStmt = (AssignStmt) stmt;
						if (stmt.toString().contains("<java.util.List: java.lang.Object get(int)>") ||
								stmt.toString().contains("<java.util.ArrayList: java.lang.Object get(int)>")){
							
							// The next instruction should be a cast to the actual object
							Stmt nextStmt = (Stmt)iter.next();
							if (nextStmt instanceof AssignStmt){
								assignStmt = (AssignStmt) nextStmt;
								if (assignStmt.getRightOp() instanceof CastExpr){
									if (assignStmt.getRightOp().getType() instanceof RefType){
										RefType refType = (RefType)assignStmt.getRightOp().getType();
										String className = refType.getClassName();
										logger.info("Class added to List.get {} ", className);
									}
								}
							}
						}
						*/
						
					} //else if (stmt instanceof Cast)
					
				 	
				}
				if (foundInMethod){ //For performance limit to one per class
					//break; //There may be multiple adds
				}
			}
		}
		return addMethodSignatures;
	}
	
	/**
	 * Return base class of the get added to collection 
	 * @param units
	 * @param start
	 * @return
	 */
	private String findCasting(Unit[] units, int start){
		//TODO fix this its really hacky and doesnt work for all cases
			Stmt stmt = (Stmt)units[start];
			if (stmt instanceof InvokeStmt){
				Value value = ((InvokeStmt) stmt).getInvokeExprBox().getValue();
				if (value instanceof InterfaceInvokeExpr){
					Value arg0 = ((InterfaceInvokeExpr)value).getArg(0);
					//if (arg0 instanceof ImmediateBox){
					//	Value arg0Value = ((ImmediateBox)arg0).getValue();
						if (arg0 instanceof JimpleLocal){
							String variableName = ((JimpleLocal)arg0).getName();
							
							//Casting should be next
							Stmt castStmt = (Stmt)units[start-1];
							if (castStmt instanceof AssignStmt){
								//left equal previous variable?
								
								AssignStmt as = (AssignStmt)castStmt;
								Value left = as.getLeftOp();
								if (left instanceof JimpleLocal){
									if (((JimpleLocal)left).getName().equals(variableName)){
										
										//Next previous instruction shoudl be our target
										Stmt modelRef = (Stmt)units[start-2];
										if (modelRef instanceof AssignStmt){
											AssignStmt assignStmt = (AssignStmt)modelRef;
											if (!assignStmt.containsInvokeExpr()){
												return null;
											}
											SootMethodRef methodRef = assignStmt.getInvokeExpr().getMethodRef();
											SootMethod sm = methodRef.resolve();
											
											String methodSignatureTag = PostProcessor.getSignatureFromTag( sm.getTags());

											//If there is a generic type and the return is an object 
											//then we grab the generic from the class
											boolean isType = new ModelExtraction().isReturnATypeParameter(methodSignatureTag);
											if (isType){ //Its a type parameter so pull it from the class signature
											
												Type retType = methodRef.returnType();
												
												//If the ret type is an Object than look at the class generics to see if this is right
												SootClass sc = methodRef.declaringClass();
												String sig = PostProcessor.getSignatureFromTag(sc.getTags());
												ModelExtraction me = new ModelExtraction();
												List<String> typeClassNames = me.getClassTypeParameters(sig);
												if (retType instanceof RefType && 
														((RefType)retType).getClassName().equals("java.lang.Object")
														&& !typeClassNames.isEmpty()){
													return typeClassNames.get(0);
												}
											}
										}
									}
								}
								
							}
						}
				}

			}
			return null;
	}
	
	
	private boolean isAddMethod(Stmt stmt){
		return stmt.toString().contains("<java.util.List: boolean addAll(java.util.Collection)>") ||
				stmt.toString().contains("<java.util.List: boolean add(java.lang.Object)>");
	}


	private String makeDefaultSignatureConstructor(String className){
		AndroidMethod am = new AndroidMethod("<init>", "void", className);
		return am.getSignature();
	}
	

	
	/**
	 * 
	 * @param baseClassName Never changes, the original class
	 * @param sootClass Class which is baseClassName or parent
	 * @param sources All signatures to be used for sources in next pass
	 * @return
	 */
	private List<String> getSourcesFromModel(String baseClassName, SootClass sootClass, List<String> sources){
		String className = sootClass.getName();
		if (className.equals("java.lang.Object")){
			return sources;
		} else {
			//Add all possible sources to the list
			sources.addAll(getSourcesFromSootClassModel(baseClassName, sootClass));
			
			if (!sootClass.hasSuperclass()){
				return sources;
			} else {
				//Check for parent methods
				SootClass superClass = sootClass.getSuperclass();
				return getSourcesFromModel(baseClassName, superClass, sources);
			}
		}
	}
	
	private List<String> getSourcesFromSootClassModel(String baseClassName, SootClass model){
		
		List<String> sources  = new ArrayList<String>();
		for (SootMethod method : model.getMethods()){
			String methodName = method.getName();
			
			//If not a void return type then grab it
			if (!(method.getReturnType() instanceof VoidType)) {
			//if (shouldAcceptMethod(methodName)){
				String currentClass = method.getDeclaringClass().getName();
				
				//infoflow needs toplevel signature
				String sig = method.getSignature().replace(currentClass, baseClassName);
				sources.add(sig);
			}
		}
		
		return sources;
	}
	
	/**
	 * Helper to determine which type of method we want to accept.
	 * @param methodName
	 * @return
	 */
	private boolean shouldAcceptMethod(String methodName){
		return methodName.toLowerCase().startsWith("get") || 
				methodName.toLowerCase().startsWith("is");
		
	}
	
	/**
	 * Extract model class from this statement
	 */
	private String extractModelClassName(ResultSinkInfo sink){

		String className = null;
		if (sink.getSink() instanceof InvokeStmt){
			className = extractModelFromInvokeStmt((InvokeStmt) sink.getSink());

		} else if (sink.getSink() instanceof AssignStmt){
			
			SootMethod m = sink.getMethod();
			
			//m.get
			className = extractModelFromAssignStmt(m, (AssignStmt) sink.getSink());
		}

		return className;
	}
	/*
	private SootClass extractSootClassModel(Stmt stmt){
		String className = extractModelClassName(stmt);
		if (className != null){
			return Scene.v().getSootClass(className);
		}
		
		return null;
	}
	*/
	
	/**
	 * Get the index where we can extract the class that is the model
	 * @return
	 */
	private Value getClassParameter(AbstractInvokeExpr invokeExpr){
		for (Value arg : invokeExpr.getArgs()){
			String t = arg.getType().toString();
			if (arg.getType().toString().equals("java.lang.Class")){
				return arg;
			}
		}
		return null;
	}
	
	/**
	 * Parse out the model class name from the deserialization call
	 * @param stmt
	 * @return
	 */
	private String extractModelFromAssignStmt(SootMethod m, AssignStmt stmt){
		String className = null;
	
		Value v = stmt.getRightOpBox().getValue();
		 if (v instanceof AbstractInvokeExpr){
			 
			 AbstractInvokeExpr invokeExpr = (AbstractInvokeExpr)v;
			 String sig = invokeExpr.getMethodRef().getSignature();
			 logger.debug("Signature " + sig);
			// if (modelParameterIndexLookup.containsKey(sig)){
	
			 	if (invokeExpr.getArgCount() == 1){ //assume it is static like protobuf
			 		return invokeExpr.getMethodRef().declaringClass().getName();
			 	} 
			 	
				// int parameterIndex = modelParameterIndexLookup.get(sig);
				 				 
				 Value boxVal = getClassParameter(invokeExpr);//invokeExpr.getArg(parameterIndex);
				 
				 if (boxVal != null){
					//Value boxVal = vb.getValue();
					if (boxVal instanceof JimpleLocal){
						
						String localName = ((JimpleLocal)boxVal).getName();
						 Type argType = boxVal.getType(); //Is this the class or a reference?
						 String argTypeString = argType.toString();
	
						 if (argTypeString.equals("java.util.Map")){
							 //currently not supported
						 } else if (argTypeString.equals("java.lang.Class")){
							 
							 //Just need to retrieve the value of the local variable
							 Body b = m.retrieveActiveBody();
							if (b != null){
								Unit[] units = new Unit[b.getUnits().size()];
								units = b.getUnits().toArray(units);
								for (int i=0; i<units.length; i++){
									if (units[i] instanceof AssignStmt){
										AssignStmt assignStmt = (AssignStmt)units[i];
										if (assignStmt.getLeftOp() instanceof JimpleLocal){
											//TODO this could be reassigned multiple different location in the class
											//this is muchmore complex
											if (((JimpleLocal)assignStmt.getLeftOp()).getName().equals(localName)){
												
												//If it is a class field, where is it set? 
												// (1) Local variable - just pull it straight from there
												// (2) From method parameter - need to locate all instances of method them pull from here
												// (3) From class parameter - set somewhere in the class, or 
												if (assignStmt.getRightOp() instanceof InstanceFieldRef){
													className = ((InstanceFieldRef)assignStmt.getRightOp()).getFieldRef().declaringClass().getName();
													break;
												}
											}
										}
									}
								}
								logger.trace("");
							}
						 } else {
							 className = argTypeString;
						 }
					} else if (boxVal instanceof ClassConstant){
						
						ClassConstant rt = (ClassConstant) boxVal;		
						className = convertBytecodeToJavaClassName(rt.getValue());
	
					} else {
						logger.error("Fuck if I know");
					}
				 }
			// }
			 
		 }
		 return className;
	}


	/**
	 * Load the parameter mappings
	 * @return
	 */
	private Map loadParameterIndexLookupFile(){
		Map map = null;
		try {
			JsonReader reader = new JsonReader(new FileReader("jsonFile.json"));
			Gson gson = new Gson();
			map = gson.fromJson(reader, Map.class);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}
	

	public File getEasyTaintWrapperFile() {
		return easyTaintWrapperFile;
	}


	public void setEasyTaintWrapperFile(File easyTaintWrapperFile) {
		this.easyTaintWrapperFile = easyTaintWrapperFile;
	}


}
