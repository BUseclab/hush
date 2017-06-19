package edu.bu.android.hiddendata.model;

import java.util.Collection;
import java.util.Map;

import edu.bu.android.hiddendata.DeserializeToUiPostProcessor.TMIResult;

public class Results {
	private String apkName;
	private int callGraphEdges;
	//private boolean hasObfuscation;
	
	/**
	 * There is a fromJson on the path, we are pretty confident this came directly from the network response
	 */
	private Collection<String> usedConfidenceHigh;
	
	/**
	 * A model get method is used in a UI element but we arent sure where 
	 * the get method originated. At tleast this can idenfity which potential parts of the 
	 * model are possibily displayed to user
	 */
	private Collection<String> usedConfidenceLow;

	//private Collection<String> notUsedInApp;
	//private Collection<String> hidden;
	//private Collection<String> extractedModels;

	//Given the used ones, to get hidden for each just need to exclude
	private Map<String, Integer> getMethodsInApp;
	
	private Map<String, TMIResult> cluster;
	
	public String getApkName() {
		return apkName;
	}
	public void setApkName(String apkName) {
		this.apkName = apkName;
	}
	/*
	public boolean isHasObfuscation() {
		return hasObfuscation;
	}
	public void setHasObfuscation(boolean hasObfuscation) {
		this.hasObfuscation = hasObfuscation;
	}
	*/
	public Collection<String> getUsedConfidenceHigh() {
		return usedConfidenceHigh;
	}
	public void setUsedConfidenceHigh(Collection<String> usedConfidenceHigh) {
		this.usedConfidenceHigh = usedConfidenceHigh;
	}
	/*
	public Collection<String> getHiddenGetMethodSignatures() {
		return hidden;
	}
	public void setHiddenGetMethodSignatures(
			Collection<String> hiddenGetMethodSignatures) {
		this.hidden = hiddenGetMethodSignatures;
	}
	*/
	public int getCallGraphEdges() {
		return callGraphEdges;
	}
	public void setCallGraphEdges(int callGraphEdges) {
		this.callGraphEdges = callGraphEdges;
	}
	public Map<String, Integer> getGetMethodsInApp() {
		return getMethodsInApp;
	}
	public void setGetMethodsInApp(Map<String, Integer> getMethodsInApp) {
		this.getMethodsInApp = getMethodsInApp;
	}
	public Collection<String> getUsedConfidenceLow() {
		return usedConfidenceLow;
	}
	public void setUsedConfidenceLow(Collection<String> usedConfidenceLow) {
		this.usedConfidenceLow = usedConfidenceLow;
	}
	/*
	public Collection<String> getNotUsedInApp() {
		return notUsedInApp;
	}
	public void setNotUsedInApp(Collection<String> notUsedInApp) {
		this.notUsedInApp = notUsedInApp;
	}
*/
	public Map<String, TMIResult> getCluster() {
		return cluster;
	}
	public void setCluster(Map<String, TMIResult> cluster) {
		this.cluster = cluster;
	}
}
