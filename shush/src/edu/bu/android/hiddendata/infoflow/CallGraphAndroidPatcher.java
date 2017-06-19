package edu.bu.android.hiddendata.infoflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bu.android.hiddendata.model.DeserializeToUIConfig;
import edu.bu.android.hiddendata.model.InjectionPoint;
import edu.bu.android.hiddendata.model.JsonUtils;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.tagkit.LineNumberTag;
import soot.tagkit.SourceLnPosTag;
import soot.tagkit.Tag;
import soot.util.Chain;

/**
 * Locate the Fragments used the APK 
 * 
 * @author Wil Koch
 * @see {LibraryClassPatcher} class for examples of creating and applying changes to scene
 * @see {BaseEntryPointCreator}
 */
public class CallGraphAndroidPatcher implements PreAnalysisHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static int CALLGRAPH_EDGES = 0;
	private static final String LISTVIEW_SETADAPTER_SIGNATURE = "<android.widget.ListView: void setAdapter(android.widget.ListAdapter)>";
	private static final String NOTIFYCHANGE_SUBSIGNATURE = "void notifyDataSetChanged()";
	
	private static final String PAGEVIEW_SETADAPTER_SIGNATURE = "<android.support.v4.view.ViewPager: void setAdapter(android.support.v4.view.PagerAdapter)>";
	private List<String> asyncTaskClasses = new ArrayList<String>();
	
	private List<String> activityOrFragmentClasses = new ArrayList<String>();

	private List<SootMethod> onPostExecuteMethods = null;

	private String injectionsFilePath;
	public CallGraphAndroidPatcher(){
		
	}
	public CallGraphAndroidPatcher(String injectionsFilePath){
		this.injectionsFilePath = injectionsFilePath;
	}
	
	
	public void rewire(){
		
		List<String> activityOrFragments = new ArrayList<String>();
		activityOrFragments.add("android.app.Activity");
		activityOrFragments.add("android.support.v4.app.Fragment");
		activityOrFragments.add("android.app.Fragment");

		
		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			SootClass sootClass = it.next();	
			if (!sootClass.getName().startsWith("android.") && !sootClass.getName().startsWith("java.") && hasSuperClass(sootClass, Collections.singletonList("android.os.AsyncTask"))){
				asyncTaskClasses.add(sootClass.getName());
			}
			
			//Need to look at all the classes for full coverage
			if (!sootClass.getName().startsWith("android.") && !sootClass.getName().startsWith("java.")){// && hasSuperClass(sootClass, activityOrFragments)){
				activityOrFragmentClasses.add(sootClass.getName());
			}
		}
		asyncTaskRewireTransformer();
		listViewRewire();
		
		if (injectionsFilePath != null){
			injectCode();
		}
	}
	
	boolean patched = false;

	private void injectCode(){
		DeserializeToUIConfig modelResults = JsonUtils.loadFirstPassResultFile(new File(injectionsFilePath));
		//If no injections are previously found the object does not get created in the model
		if (modelResults.getInjections() == null){ return;}
		
		for (final InjectionPoint inject : modelResults.getInjections()){
			SootClass sootClass = Scene.v().getSootClass(inject.getDeclaredClass());
			SootMethod method = sootClass.getMethodUnsafe(inject.getMethodSignature());
			
			final Body body = method.retrieveActiveBody();
			final PatchingChain<Unit> units = body.getUnits();
			final LocalGenerator generator = new LocalGenerator(body);
			patched = false;
			for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
				final Unit u = iter.next();
				u.apply(new AbstractStmtSwitch() {
					
					 public void defaultCase(Object obj)
					    {
						 
						 if (obj.toString().equals(inject.getTargetInstruction())){
							 
							 if (obj instanceof InvokeStmt){
								 
								 List<Unit> chainToInsert = new ArrayList<Unit>();
								 InvokeStmt invokeStmt = (InvokeStmt) obj;
								 SpecialInvokeExpr expr = (SpecialInvokeExpr) invokeStmt.getInvokeExprBox().getValue();					 
								 
					    		//Create new constructor for model
					    		SootClass classToInject = Scene.v().getSootClassUnsafe(inject.getClassNameToInject());
					    		SootMethod getViewMethod = classToInject.getMethodUnsafe("void <init>()");
					    		
					    		//For new injected code have the same line number as the injection point
						    	Tag tag = (SourceLnPosTag)invokeStmt.getTag("SourceLnPosTag");
						    	if(tag== null){
						    		//get line from bytecode
						    		tag = (LineNumberTag)invokeStmt.getTag("LineNumberTag");
							    	
						    	}
	
	//TODO how to make this more flexible for different injectino types?			    			
					    		
					    		//Create new class
					    		//$r1 = new com.foo.bar.MyClass
					    		NewExpr newClass = Jimple.v().newNewExpr(RefType.v(classToInject));
								Local tempLocal = generator.generateLocal(RefType.v(classToInject));			
					    		chainToInsert.add(Jimple.v().newAssignStmt(tempLocal, newClass));
					    		
					    		SpecialInvokeExpr invokeConstructor = Jimple.v().newSpecialInvokeExpr(tempLocal, getViewMethod.makeRef());
					    		Stmt constructorStmt = Jimple.v().newInvokeStmt(invokeConstructor);
					    		constructorStmt.addTag(tag);
					    		chainToInsert.add(constructorStmt);
					    		
					    		
					    		//Create the call to view
					    		SootClass listClass = Scene.v().getSootClassUnsafe("java.util.List");
					    		SootMethod listAddMethod = listClass.getMethodUnsafe("boolean add(java.lang.Object)");
					    		
					    		List<Value> addMethodArgs = new ArrayList<Value>();
					    		addMethodArgs.add(tempLocal);
				    			Value refVal = expr.getBaseBox().getValue();
					    		InterfaceInvokeExpr getViewStmt = Jimple.v().newInterfaceInvokeExpr((Local)refVal, listAddMethod.makeRef(), addMethodArgs);
					    		chainToInsert.add(Jimple.v().newInvokeStmt(getViewStmt));
					    		
					    		//Patch
					    		units.insertAfter(chainToInsert, invokeStmt);
					    		patched = true;
							 }
						    		
						 }
					    }

					
				});
			}
			
			if (patched){
				//Unit[] u = new Unit[body.getUnits().size()];
			//	u = body.getUnits().toArray(u);
			//	logger.trace("");
			}

		}
	}
	
	private void pageViewRewire(){
		
	}

	
	/**
	 * Go up the hierarchy and see if it has a parent 
	 * @param sootClass
	 * @param superClass
	 * @return
	 */
	private boolean hasSuperClass(SootClass sootClass, List<String> superClasses){
		String className = sootClass.getName();
		if (className.equals("java.lang.Object")){
			return false;
		} else if (superClasses.contains(className)){
			return true;
		} else {
			if (!sootClass.hasSuperclass()){
				return false;
			} else {
				SootClass superClass = sootClass.getSuperclass();
				return hasSuperClass(superClass, superClasses);
			}
		}
	}
	
	private SootMethod getCorrespondingonPostExecuteMethod(SootMethod doInBackgroundMethod){
		Type returnType = doInBackgroundMethod.getReturnType();
		for (SootMethod onPostExecuteMethod : onPostExecuteMethods){
			
			//By the signature, it has only 1 param
			if (onPostExecuteMethod.getParameterTypes().size() > 0){
				Type paramType = onPostExecuteMethod.getParameterTypes().get(0);
				if (paramType.equals(returnType)){
					return onPostExecuteMethod;
				}
			}
		}
		return null;
	}

	private void rewireDoInBackground(SootClass currentClass, final SootMethod doInBackgroundMethod){
		
		final Body body = doInBackgroundMethod.retrieveActiveBody();
		final Local local = body.getThisLocal();
		final SootMethod onPostExecuteMethod = getCorrespondingonPostExecuteMethod(doInBackgroundMethod);
		if (onPostExecuteMethod == null){return;}
		
		final PatchingChain<Unit> units = body.getUnits();
		
		for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			final Unit u = iter.next();
			u.apply(new AbstractStmtSwitch() {
				
				//TODO fix case for more complex return types
			    public void caseReturnStmt(ReturnStmt stmt)
			    {
			    	Value value = stmt.getOpBox().getValue();
			    	if (!(value instanceof NullConstant)){
			    		InvokeExpr vie = Jimple.v().newVirtualInvokeExpr(local, onPostExecuteMethod.makeRef(), value);
			    		InvokeStmt onPostExecuteStatement = Jimple.v().newInvokeStmt(vie);
			    	
			    		units.insertBefore(onPostExecuteStatement, stmt);
			    	}
			    }
				
			});
		}
	}
	private void rewireAsyncTaskClass(SootClass currentClass){
		onPostExecuteMethods = new ArrayList<SootMethod>();
		List<SootMethod> methods = currentClass.getMethods();
		//First get all postexecute methods. One will be super, other implemented
		for (SootMethod method : methods){
			if (method.getName().startsWith("onPostExecute")){
				logger.debug("onPostExecute {}", method.getSignature());
		    	onPostExecuteMethods.add(method);
			}
		}
		if (!onPostExecuteMethods.isEmpty()){
			for (SootMethod method : methods){
				//TODO replace with get by subsignature?
				if (!method.isAbstract() && method.getName().startsWith("doInBackground")){
					logger.debug("doInBackground {}", method.getSignature());
					rewireDoInBackground(currentClass, method);
				}
			}
		}
	}
	
	/**
	 * Look at every unit to find setAdapter
	 * For Lists and ViewPagers
	 * @param sootClass
	 */
	private void rewireListView(SootClass sootClass){
		List<SootMethod> methods = sootClass.getMethods();
		for (SootMethod method : methods){
			try {
				final Body body = method.retrieveActiveBody();
				
				final PatchingChain<Unit> units = body.getUnits();
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					u.apply(new AbstractStmtSwitch() {
					    public void caseInvokeStmt(InvokeStmt stmt)
					    {
					    	Value value = stmt.getInvokeExprBox().getValue();
					    	if (value instanceof JVirtualInvokeExpr){
					    		JVirtualInvokeExpr expr = (JVirtualInvokeExpr)value;
					    		
					    		//Create the arguments for the method call
	
					    		List<Value> getViewArgs = new ArrayList<Value>();
					    		getViewArgs.add(IntConstant.v(0));
					    		getViewArgs.add(NullConstant.v());
					    		getViewArgs.add(NullConstant.v());
					    		
					    		List<Value> instantiateItemArgs = new ArrayList<Value>();
					    		instantiateItemArgs.add(NullConstant.v());
					    		instantiateItemArgs.add(IntConstant.v(0));
					    		
					    		//Get method we want to call, need to a local reference so need to wait to create expression
					    		SootClass adapter = Scene.v().getSootClassUnsafe("android.widget.Adapter");
					    		SootMethod getViewMethod = adapter.getMethodUnsafe("android.view.View getView(int,android.view.View,android.view.ViewGroup)");
					    		
					    		SootClass pageViewAdapter = Scene.v().getSootClassUnsafe("android.support.v4.view.PagerAdapter");
					    		SootMethod instantiateItemMethod = pageViewAdapter.getMethodUnsafe("java.lang.Object instantiateItem(android.view.ViewGroup,int)");
					    		SootMethod instantiateItemMethod2 = pageViewAdapter.getMethodUnsafe("java.lang.Object instantiateItem(android.view.View,int)");

					    		
					    		
					    		//Look for when adapter is set
					    		if (expr.getMethodRef().getSignature().equals(LISTVIEW_SETADAPTER_SIGNATURE)){
					    			
					    			Value argValue = expr.getArgBox(0).getValue();
					    			
						    		//Create the call to view
						    		InterfaceInvokeExpr getViewStmt = Jimple.v().newInterfaceInvokeExpr((Local)argValue, getViewMethod.makeRef(), getViewArgs);
						    		
						    		//Patch
						    		units.insertAfter(Jimple.v().newInvokeStmt(getViewStmt), stmt);
						    		
						    	//There is a notifydatasetchange for the PagerAdapter and BaseAdapter classes
					    		} else if (expr.getMethodRef().getSubSignature().getString().equals(NOTIFYCHANGE_SUBSIGNATURE)){
					    			
					    			//TODO should make sure this is of BaseAdapter
					    			//expr.getMethodRef().declaringClass()
					    			Value refValu = expr.getBaseBox().getValue();
					    			
					    			InterfaceInvokeExpr getViewStmt;
					    			//Look to find out what the callee is
					    			if (hasSuperClass(expr.getMethodRef().declaringClass(), Collections.singletonList("android.support.v4.view.PagerAdapter"))){
					    				List<Unit> chain = new ArrayList<Unit>();
					    				
					    				if (instantiateItemMethod != null){
					    				chain.add(Jimple.v().newInvokeStmt(
					    						Jimple.v().newVirtualInvokeExpr((Local)refValu, instantiateItemMethod.makeRef(), instantiateItemArgs)));
					    				}
					    				try {
						    				if (instantiateItemMethod2 != null){
							    				chain.add(Jimple.v().newInvokeStmt(
							    						Jimple.v().newVirtualInvokeExpr((Local)refValu, instantiateItemMethod2.makeRef(), instantiateItemArgs)));
							    			}
					    				} catch(Exception e){
					    					logger.error(e.getMessage());
					    				}
					    				
							    		units.insertAfter(chain, stmt);

					    			} else {
					    				//Create the call to view
					    				getViewStmt = Jimple.v().newInterfaceInvokeExpr((Local)refValu, getViewMethod.makeRef(), getViewArgs);
					    				//Patch
							    		units.insertAfter(Jimple.v().newInvokeStmt(getViewStmt), stmt);
							    		
							    	}
					    			
					    			
					    			
						    		
					    		} else if (expr.getMethodRef().getSignature().equals(PAGEVIEW_SETADAPTER_SIGNATURE)){
					    			
					    			try{
						    			Value argValue = expr.getArgBox(0).getValue();	
							    		//Create the call to view
						    			VirtualInvokeExpr invokeExpr = null;//Jimple.v().newVirtualInvokeExpr((Local)argValue, instantiateItemMethod.makeRef(), instantiateItemArgs);
							    		
						    			if (instantiateItemMethod != null){
						    				invokeExpr =
						    						Jimple.v().newVirtualInvokeExpr((Local)argValue, instantiateItemMethod.makeRef(), instantiateItemArgs);
						    				
						    			} else {
						    				invokeExpr =
								    				Jimple.v().newVirtualInvokeExpr((Local)argValue, instantiateItemMethod2.makeRef(), instantiateItemArgs);
								    	}
						    				
							    		//Patch
							    		units.insertAfter(Jimple.v().newInvokeStmt(invokeExpr), stmt);
					    			} catch(Exception e){
				    					logger.error(e.getMessage());
				    				}
					    		}
					    		
					    	}
					    	
					    }
					});
				}
				//logger.debug("{}", method.retrieveActiveBody());
			} catch (Exception e){
				continue;
			}
		}
	}
	
	private void asyncTaskRewireTransformer(){
		logger.info("Performing Async patching...");
		for (String className : asyncTaskClasses) {
			SootClass sootClass = Scene.v().getSootClass(className);
			//SootClass sootClass = Scene.v().forceResolve(className, SootClass.BODIES);
			//sootClass.setApplicationClass();
			logger.debug("Async class {}", sootClass.getName());
			rewireAsyncTaskClass(sootClass);
		}
	}
	
	
	/**
	 * For each notifydatasetchange
	 * and setadapter
	 * inject getView method after so it can be reached
	 */
	private void listViewRewire(){
		for (String className : activityOrFragmentClasses) {
			SootClass sootClass = Scene.v().getSootClass(className);
			rewireListView(sootClass);
		}
	}
	
	public void printAsyncBodies(){
		for (String className : asyncTaskClasses) {
			SootClass sootClass = Scene.v().getSootClass(className);
			logger.debug("*****************************Async {}", sootClass);

			List<SootMethod> methods = sootClass.getMethods();

			for (SootMethod method : methods){
				if (method.getName().startsWith("doInBackground")){
					Body body = method.retrieveActiveBody();
					logger.trace("{}", body);

				}
			}
		}
	}

	@Override
	public void onBeforeCallgraphConstruction() {
		rewire();
	}

	@Override
	public void onAfterCallgraphConstruction() {
		//Once the callgraph is created set the number edges to be reported
		//in results, hacky i know
		CALLGRAPH_EDGES = Scene.v().getCallGraph().size();
		logger.trace("");
	}

}
