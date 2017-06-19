package edu.bu.android.hiddendata;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import edu.bu.android.hiddendata.infoflow.CallGraphAndroidPatcher;
import edu.bu.android.hiddendata.model.DeserializeToUIConfig;
import edu.bu.android.hiddendata.model.JsonUtils;
import edu.bu.android.hiddendata.model.Results;
import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.util.Chain;

/**
 * 
 * @author William Koch
 *
 */
public class DeserializeToUiPostProcessor extends PostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(DeserializeToUiPostProcessor.class.getName());

	private InfoflowResults results;
	private DeserializeToUIConfig stage2Config;
	File resultsFile;
	/**
	 * 
	 * @param context The application context
	 * @param netToJsonFlowResults The results from the flow analsysi from the network to deserialize methods
	 */
	public DeserializeToUiPostProcessor(File resultsFile , File netToJsonResultsFile, SetupApplication context, InfoflowResults netToJsonFlowResults ){
		super(context);
		this.results = netToJsonFlowResults;
		this.resultsFile = resultsFile;
		//Obtained from the first pass where all the models are found
		this.stage2Config = JsonUtils.loadFirstPassResultFile(netToJsonResultsFile);
	}
	
	/**
	 * We check if the source is not 
	 */
	public void findHiddenData(){
		
		Map<String, TMIResult> cluster = new HashMap<String, TMIResult>();

		Set<String> foundSourcesConfidentHigh = new HashSet<String>();
		Set<String> foundSourcesConfidentLow = new HashSet<String>();

		Set<String> hiddenMethods = new HashSet<String>();
		hiddenMethods.addAll(stage2Config.getGetMethodSignatures());
		
		if (results != null){

			/*
			 * There are currently 4 different types of flows
			 * 
			 * fromJson -> UI element 	If this is found nothing more is needed
			 * 
			 * List()   -> UI element	
			 * fromJson -> List.add()	Is this list that is added the same as the list who has a path to a UI element?
			 */
			//Just add all the signatures of the sources into a list
			Map<String, String> mapping = stage2Config.getDeserializeToModelMapping();

			for (ResultSinkInfo foundSink : results.getResults().keySet()) {
				
				Set<String> modelDataUsedInSink = new HashSet<String>();
				Set<ModelDataAndClass> modelDataAndClass= new HashSet<ModelDataAndClass>();

				Set<String> originalDeserializers = new HashSet<String>();
				boolean isOriginalDeserializer = false;
				for (ResultSourceInfo foundSource : results.getResults().get(foundSink)) {
					
					String src = foundSource.getSource().toString();
					//Make sure its form original source
					if (isOriginalSource(foundSource.getSource().toString())){
						
						
						//If fromJson do nothing everything is great
						//otherwise

						//First try seeign if the model is included in the path and just walk backwards
						//If using something with lenth > 1 
						//or contextsensitive
						Set<String>  modelDataUsed = getTaintedModelMethodFromFlow(foundSource);
						
						//If original sink
						if (foundSource.getSource().toString().contains("fromJson")){
							isOriginalDeserializer = true;
						}
						String sig = PostProcessor.makeSignature(foundSource);
						if (isDeserializeMethod(sig)){
							originalDeserializers.add(sig);
							isOriginalDeserializer = true;
							
							
						}
						
						ModelDataAndClass mdac = new ModelDataAndClass();
						mdac.modelData = getSignatuteFromStmt(foundSource.getSource(), false);
						mdac.modelDataClass = getDeclaringClass(foundSource.getSource());
						
						modelDataAndClass.add(mdac);
						//Otherwise as a last resort look directly at the 
						if (modelDataUsed.isEmpty()){
							
						}
						//foundSources.addAll(getMethodsUsed);
						modelDataUsedInSink.addAll(modelDataUsed);
					}
				} //end source
				
				if (!originalDeserializers.isEmpty()){
					
					/*
					 * What we are trying to do here is cluster the results.
					 * When we arent using contextsentistive flows (which is most of the time because of memory issues)
					 * we have many results pointing to a single UI sink.
					 * 
					 * We cluster the results based on the originating deserialize sources.
					 * The model data that belongs to the deserialize class will be grouped together
					 */
					if (mapping != null){//backwards compatible
										
						for (String d : originalDeserializers){
							
						//	Set<String> hidden = new HashSet<String>();
							Set<String> shown = new HashSet<String>();
							
							String deserializedModel = mapping.get(d);
							
							if (deserializedModel != null){
								for (ModelDataAndClass m : modelDataAndClass){
									//FIXME this check will fail if nested objects are used
									if (m.modelDataClass.equals(deserializedModel)){
										shown.add(m.modelData);
										
										
									}
								}
								
								if (!cluster.containsKey(d)){
									TMIResult tmi = new TMIResult();
									tmi.model = deserializedModel;
									tmi.hidden = new HashSet<String>();
									tmi.shown = new HashSet<String>();
									cluster.put(d, tmi);
								}
								cluster.get(d).shown.addAll(shown);
								//cluster.get(d).hidden.addAll(hidden);
							}
						}	
					}
					
				}
				//Is a source of fromJson? If so then everything is cool
				//we will add all the getMethods found here
				if (isOriginalDeserializer){
					foundSourcesConfidentHigh.addAll(modelDataUsedInSink);
				} else {
					//Not really confident
					foundSourcesConfidentLow.addAll(modelDataUsedInSink);
				}
			} //end sink
			
			//Figure out hidden
			for (String d : cluster.keySet()){
				String deserializedModel = mapping.get(d);
				Set<String> hidden = getModelDataByClassName(deserializedModel);
				
				Iterator<String> it = hidden.iterator();
				while (it.hasNext()){
					String h = it.next();
					if (cluster.get(d).shown.contains(h)){
						it.remove();
					}
				}
				cluster.get(d).hidden = hidden;
			}
			
			
			
			
			printCluster(cluster);
			
			//From all of the method signatures, remove the ones we found
			//whats left is hidden
			for (String f : foundSourcesConfidentHigh ){
				logger.info("(HIGH) Method used! {}", f);
				hiddenMethods.remove(f);
			}
			for (String f : foundSourcesConfidentLow ){
				logger.info("(Low) Method used! {}", f);
				hiddenMethods.remove(f);
			}
			
		}
		
		
		for (String hidden : hiddenMethods){
			logger.info("HIDDEN {}", hidden);
		}
		
		Map<String, Integer> getMethodsInApp =  locateAllGetMethods();
		
		
		//Write the results
		Results results = new Results();
		results.setApkName(new File(context.getApkFileLocation()).getName());
		results.setCallGraphEdges(CallGraphAndroidPatcher.CALLGRAPH_EDGES);
		results.setGetMethodsInApp(getMethodsInApp);
		//results.setHiddenGetMethodSignatures(hiddenMethods);
		results.setUsedConfidenceHigh(foundSourcesConfidentHigh);
		results.setUsedConfidenceLow(foundSourcesConfidentLow);
		results.setCluster(cluster);
		JsonUtils.writeResults(resultsFile, results);		
		
	}
	public class ModelDataAndClass{
		public String modelData;
		public String modelDataClass;
	}
	//private Set<String> getDataOriginatingFromDeserializer(){
		
	//}
	private void printCluster(Map<String, TMIResult> cluster){
		logger.info("The following are used for the given response deserialized");
		for (String key : cluster.keySet()){
			logger.info("Deserialized " + key);
			TMIResult tmi = cluster.get(key);
			logger.info("{} Shown, {} Hidden", tmi.shown.size(), tmi.hidden.size());
			logger.info("\tShown");
			for (String show : tmi.shown){
				logger.info("\t" + show);
			}
			logger.info("\tHidden");
			for (String hide : tmi.hidden){
				logger.info("\t" + hide);
			}
		}
	}
	private boolean isDeserializeMethod(String sig){
		for (String originalDeserialize : stage2Config.getOriginalSinks()){
			if (sig.contains(originalDeserialize)){
				return true;
			}
		}
		return false;
	}
	private Set<String> notUsed ( Map<String, Integer> methodsUsed){
		Set<String> notUsed = new HashSet<String>();
		
		return notUsed;
	}
	private void writeResults(){
		Results results = new Results();
		
		Scene.v().getCallGraph().size();
	}
	
	
	//TODO this will not work for nested clases
	//TODO change the config to have model data mapped to the root model class
	private Set<String> getModelDataByClassName(String className){
		Set<String> data = new HashSet<String>();
		
		for (String d : stage2Config.getGetMethodSignatures()){
			if (d.contains(className)){
				data.add(d);
			}
		}
		return data;
	}
	/**
	 * Search the entire APK for instances of any of the get methods occuring so we can 
	 * get an idea of coverage
	 * @return
	 */
	private Map<String, Integer> locateAllGetMethods(){
		
		//Ideally for readability we want this in ascending order so we know which
		//methods are not used at all
		SortedMap<String, Integer> methodOccurences = new TreeMap<String, Integer>();
		
		//Init method occurences to 0
		for (String getClassName : stage2Config.getGetMethodSignatures()){
			methodOccurences.put(getClassName, 0);
		}

		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			final SootClass sc = it.next();
			if (isFrameworkClass(sc.getName())){
				continue;
			}
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
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					
					//For this 
					for (String getClassName : stage2Config.getGetMethodSignatures()){
						if (u.toString().contains(getClassName)){
							//if (methodOccurences.containsKey(getClassName)){
							int c = methodOccurences.get(getClassName);
							methodOccurences.put(getClassName, c+1);
							//} else {
							//	methodOccurences.put(getClassName, 1);
							//}
							break;
						}
					}
				}
			}
		}
		return methodOccurences;
	}
	/**
	 * When we find a flow we must find out which method from the model was tainted and 
	 * ended up at the sink. This will identify data that is displayed to the user.
	 * @return Return a list of signatures because we want to find all the get methods that may have been accessed to get to this sink
	 */
	private Set<String> getTaintedModelMethodFromFlow(ResultSourceInfo source){
		Set<String> tainted = new HashSet<String>();

		//No path, this source is the only thing
		if (source.getPath().isEmpty()){
			Stmt stmt = source.getStmt();

			String signature = getSignatuteFromStmt(stmt, false);
			
			if (signature != null){
				if (stage2Config.getGetMethodSignatures().contains(signature)){
					//return signature;
					tainted.add(signature);
				}
			}
		}
		
		Collections.reverse(source.getPath());
		for (Stmt stmt : source.getPath()){
			
			String signature = getSignatuteFromStmt(stmt, false);
			
			if (signature != null){
				if (stage2Config.getGetMethodSignatures().contains(signature)){
					//return signature;
					tainted.add(signature);
				}
			}
		}
		return tainted;
	}
	//FIXME this is a dup of the one is PostProcess
	private String getSignatuteFromStmt(Stmt stmt, boolean useSub){

		String signature = null;
		if (stmt instanceof AssignStmt){
			AssignStmt assignStmt = (AssignStmt) stmt;
			if (assignStmt.containsInvokeExpr()){
				if (useSub){
					signature = assignStmt.getInvokeExpr().getMethod().getSubSignature();
				} else {
					signature = assignStmt.getInvokeExpr().getMethod().getSignature();
				}
			} else {
				Value value = assignStmt.getRightOp();
				if (value instanceof JInstanceFieldRef){
					JInstanceFieldRef ref = (JInstanceFieldRef) value;
					signature = ref.getFieldRef().toString();
				}
			}
		} else if (stmt instanceof InvokeStmt){
			if (useSub){
				signature = ((InvokeStmt)stmt).getInvokeExpr().getMethod().getSubSignature();
			} else {
				signature = ((InvokeStmt)stmt).getInvokeExpr().getMethod().getSignature();
			}
		} 
		
		
		return signature;
	}
	//FIXME this is a dup of the one is PostProcess
	private String getDeclaringClass(Stmt stmt){
		
		if (stmt.containsInvokeExpr()){

			return stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
		} 
		else if (stmt.containsFieldRef()){
			return stmt.getFieldRef().getField().getDeclaringClass().getName();
			
		} else {
			
		}
		return null;
	}
	/**
	 * Because of the way the flows are reported there could be errors from parsing so dont do that, 
	 * just see if the signature is present in any sources found.
	 * 
	 * @param foundSources	List of all the sources found in the analysis
	 * @param source	A source from the model
	 * @return
	 */
	private boolean found(List<String> foundSources, String source){
		for (String foundSource : foundSources){
			if (foundSource.contains(source)){
				return true;
			}
		}
		return false;
	}
	
	
	private void displayResults(HashMap<String, Integer> references, List<String> hiddenValues){
		logger.info("All references found in code:");
		//logger.info("\t{}", references);
		logger.info("These appear to be hidden from user (references in app). If no references found in app then (1) sent to phone but never used, (2) not used at all.");
		for (String signature : hiddenValues){
			int referencesInApp = references.get(signature);
			logger.info("\t({}) {} ", referencesInApp, signature );
		}
		
	}
	
	public class ClusterResult{
		public String deserializeMethod = null;
		public Set<String> modelData;
	}
	
	public class TMIResult {
		String model;
		public Set<String> hidden;
		public Set<String> shown;
		
	}
	
}
