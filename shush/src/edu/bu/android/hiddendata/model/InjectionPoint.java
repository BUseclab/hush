package edu.bu.android.hiddendata.model;

public class InjectionPoint {
	private String declaredClass;
	private String methodSignature;
	private String targetInstruction;
	private String classNameToInject;
	
	public String getDeclaredClass() {
		return declaredClass;
	}
	/**
	 * This is the class the code should be injected in
	 * @param declaredClass
	 */
	public void setDeclaredClass(String declaredClass) {
		this.declaredClass = declaredClass;
	}
	public String getMethodSignature() {
		return methodSignature;
	}
	
	/**
	 * This is the method the code should be injected in
	 * @param methodSignature
	 */
	public void setMethodSignature(String methodSignature) {
		this.methodSignature = methodSignature;
	}
	public String getTargetInstruction() {
		return targetInstruction;
	}
	/**
	 * Set the target instruction for which the injected code should be placed afterwards
	 * @param targetInstruction
	 */
	public void setTargetInstruction(String targetInstruction) {
		this.targetInstruction = targetInstruction;
	}
	public String getClassNameToInject() {
		return classNameToInject;
	}
	public void setClassNameToInject(String classNameToInject) {
		this.classNameToInject = classNameToInject;
	}
	
	
}
