package edu.bu.android.hiddendata.model;

import java.util.List;

public class Model {

	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public List<String> getGetMethodSignatures() {
		return getMethodSignatures;
	}
	public void setGetMethodSignatures(List<String> getMethodSignatures) {
		this.getMethodSignatures = getMethodSignatures;
	}
	private String className;
	private List<String> getMethodSignatures;
}
