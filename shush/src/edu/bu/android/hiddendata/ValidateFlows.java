package edu.bu.android.hiddendata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Main;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;
import soot.util.Chain;

/**
 * Search for all references of the deserialize model object so it can be used as validation if the flows are found.
 * 
 * This will help identify possible flows that are missed. 
 * 
 * @author William Koch
 */
@Deprecated //To slow
public class ValidateFlows {
	private static final Logger logger = LoggerFactory.getLogger(ValidateFlows.class.getName());
	
	private List<String> foundJsonDeserializeSources;
	private SetupApplication context;
	
	private HashMap<String, Integer> methodHistogram = new HashMap<String, Integer>();
	
	public ValidateFlows(SetupApplication context, List<String> methodsToSearchFor){
		this.foundJsonDeserializeSources = methodsToSearchFor;
		this.context = context;
		initMethodHistogram();

	}
	
	private void initMethodHistogram(){
		for (String method : foundJsonDeserializeSources){
			methodHistogram.put(method, 0);
		}
	}
	
	public void validate(){
		soot.G.reset();

		context.initializeSoot();
	
        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
				
					String thisClassName = "";
					try {
						Local thisLocal = b.getThisLocal();
						Type type = thisLocal.getType();
						thisClassName = type.toString();
					} catch (Exception e){
						
					}
					processBody(b,thisClassName );
				}
		}));
		
       PackManager.v().runPacks();
	}
	
	private void processBody(Body body, String thisName){
		Iterator<Unit>it =  body.getUnits().snapshotIterator();
		while(it.hasNext()){
			Unit unit = it.next();
			if (doesUnitMatchFoundSources(unit, thisName)){
				//should we do something here?
				
			}
		}
	}

	
	public boolean doesUnitMatchFoundSources(Unit unit, String thisName){
		
		boolean found = false;
		for (String methodSig : foundJsonDeserializeSources){
			if (unit.toString().contains(methodSig)){
				int c = methodHistogram.get(methodSig);
				methodHistogram.put(methodSig, c+1);
				logger.info("Class: {} Found source in {} ", thisName, unit.toString());
				return true;
			}
		}
		return found;
	}
	
	
	
	public HashMap<String, Integer> getResults(){
		return methodHistogram;
	}
	

}
