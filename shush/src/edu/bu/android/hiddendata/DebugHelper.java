package edu.bu.android.hiddendata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.util.Chain;

public class DebugHelper implements PreAnalysisHandler{
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void dumpJimple(){


		Chain<SootClass> classes = Scene.v().getClasses();
		Iterator<SootClass> it = classes.iterator();
		while (it.hasNext()){
			final SootClass sc = it.next();
			if (PostProcessor.isFrameworkClass(sc.getName())){
				continue;
			}
			for (SootMethod method : sc.getMethods()){
				if (!method.hasActiveBody()){
					logger.warn("{} {} is not active body", sc.getName(), method.getSignature());
					continue;
				}
				
				logger.info("{} {}", sc.getName(),  method.retrieveActiveBody());

			}
		}
	}

	@Override
	public void onBeforeCallgraphConstruction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAfterCallgraphConstruction() {
		dumpJimple();

	}
}
