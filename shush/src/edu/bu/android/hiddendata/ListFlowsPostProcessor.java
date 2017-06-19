package edu.bu.android.hiddendata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bu.android.hiddendata.PostProcessor.ListFlow;
import edu.bu.android.hiddendata.model.DeserializeToUIConfig;
import edu.bu.android.hiddendata.model.InjectionPoint;
import edu.bu.android.hiddendata.model.JsonUtils;
import soot.SootClass;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

public class ListFlowsPostProcessor extends PostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ListFlowsPostProcessor.class.getName());

	private InfoflowResults results;
	private DeserializeToUIConfig modelResults;
	private File resultsFile;
	File sourceSinkFileDeserializeToUI;

	/**
	 * 
	 * @param context
	 * @param results
	 * @param netToModelResultsFile 	So we can fill in injectins 
	 */
	public ListFlowsPostProcessor(SetupApplication context,  InfoflowResults results, File netToModelResultsFile, File sourceSinkFileDeserializeToUI ) {
		super(context);
		this.results = results;
		this.resultsFile = netToModelResultsFile;
		this.modelResults = JsonUtils.loadFirstPassResultFile(netToModelResultsFile);
		this.sourceSinkFileDeserializeToUI = sourceSinkFileDeserializeToUI;

	}

	public void process(){
		if (results == null){
			return;
		}
		
		//Track all the objects that are added to lists
		HashMap<String, ListFlow> addMethodParameterClassNames = new HashMap<String, ListFlow>();
		
		for (ResultSinkInfo sink : results.getResults().keySet()) {
			
			//Look at the sources and see which are actually specified in our source sink file
			//so we can filter out possible onces found from the callbacks enabled
			for (ResultSourceInfo source : results.getResults().get(sink)) {
				
				//Make sure its form original source
				if (isOriginalSource(source.getSource().toString())){

						ListFlow listFlow = new ListFlow();
						listFlow.source = source;
						listFlow.sink = sink;
						
						String sig = getSignature(sink.getDeclaringClass(), sink.getSink());
						String modelClass = modelResults.getModelToListSignatureMapping().get(sig);
						addMethodParameterClassNames.put(modelClass, listFlow);
				
				}
			}
		}
		
		List<String> signatures = new ArrayList<String>();
		Set<InjectionPoint> injections = new HashSet<InjectionPoint>();
		Iterator<String> it = addMethodParameterClassNames.keySet().iterator();
		while(it.hasNext()){
			String addMethodParameterClassName = it.next();
			ListFlow resultInfo = addMethodParameterClassNames.get(addMethodParameterClassName);
			
			//Right after a List constructor
			//Inject list.add(new Model())
			//TODO need to add line number to differentiat between mutlple targets
			InjectionPoint inject = new InjectionPoint();
			inject.setDeclaredClass(resultInfo.source.getDeclaringClass().getName());
			inject.setTargetInstruction(resultInfo.source.getStmt().toString());
			inject.setMethodSignature(resultInfo.source.getMethod().getSubSignature());
			inject.setClassNameToInject(addMethodParameterClassName);
			injections.add(inject);
		
			
			signatures.add(makeDefaultSignatureConstructor(addMethodParameterClassName, resultInfo.source.getDeclaringClass().getName(), resultInfo.source.getStmt().getJavaSourceStartLineNumber() ));
		}
		
		modelResults.setInjections(injections);
		JsonUtils.writeResults(resultsFile, modelResults);
		
		//Add unique specific sources to the last pass
		appendToSourceSinkFile( signatures);
		
	}
	
	private String getSignature(SootClass sc, Stmt stmt){
		AndroidMethod am = new AndroidMethod(stmt.getInvokeExpr().getMethod());
		am.setLineNumber(stmt.getJavaSourceStartLineNumber());
		am.setDeclaredClass(sc.getName());
		String signatureWithLineNumber = am.getSignature();
		return signatureWithLineNumber;
	}


	private String makeDefaultSignatureConstructor(String className, String declaredClass, int lineNumber){
		AndroidMethod am = new AndroidMethod("<init>", "void", className);
		am.setDeclaredClass(declaredClass);
		am.setLineNumber(lineNumber);
		return am.getSignature();
	}
	
	/**
	 * Append model constructors as sources
	 * @param signatures
	 */
	private void appendToSourceSinkFile(List<String> signatures){
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sourceSinkFileDeserializeToUI, true)))) {
			for (String s : signatures){
				out.println(s + " -> _SOURCE_");
			}
		}catch (IOException e) {
		    //exception handling left as an exercise for the reader
			logger.error("Cant append to source sink file " + sourceSinkFileDeserializeToUI.getAbsolutePath());
		}
	}
}
