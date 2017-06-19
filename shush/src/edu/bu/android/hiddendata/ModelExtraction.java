package edu.bu.android.hiddendata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bu.android.hiddendata.parser.SignatureParser;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.tagkit.SignatureTag;
import soot.tagkit.Tag;
import sun.reflect.generics.tree.*;

/**
 * A class for handling all sorts of model (class) extractions
 * @author Wil Koch
 *
 */
public class ModelExtraction {

	public interface OnExtractionHandler {
		public void onMethodExtracted(SootClass sootClass, SootMethod method);
		public void onFieldExtracdted(SootClass sootClass, SootField sootField);
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private OnExtractionHandler handler;
	private String modelClassName;
	private static final String PATTERN_GENERIC_CLASS = "\\(\\)[\\w/]+<\\w([\\w/]+);>;";
	 Pattern pattern = Pattern.compile(PATTERN_GENERIC_CLASS);

	public ModelExtraction(){
	}
	
	public void addHandler(OnExtractionHandler handler){
		this.handler = handler;
	}
	/**
	 * Get all the models associated with this class including get methods that return other models
	 * and also from super classes.
	 * @return
	 */
	public Set<String> getModels(String modelClassName){
		Set<String> classes = new HashSet<String>();

		if (modelClassName == null){
			return classes;
		}
		SootClass sootClass = Scene.v().getSootClass(modelClassName);
			
		//need to look at all the super methods too because although the child class can access them all
		//they are not represented in Java this way
		
		getMethodReturnClassNames(sootClass, classes, null);
		
		for (String c : classes){
			logger.debug("Class {}", c);
		}
		return classes;
	}
	
	/**
	 * Collect all the class reference types of methods recursively
	 * @param sootClass
	 * @param classes Keep track of all the classes we've visited so we don't get into a recursive loop
	 * @param superTypeSignature
	 */
	private void getMethodReturnClassNames(SootClass sootClass, Set<String> classes, ClassTypeSignature superTypeSignature){
		ClassTypeSignature childSuperTypeSignature = null;
		ClassTypeSignature currentSuperTypeSignature = null;

		//For all the classes
		for (SootClass sc : getSuperSootClasses(sootClass, true)){
			if (PostProcessor.isFrameworkClass(sc.getName())){
				continue;
			}
			classes.add(sc.getName());
			
			if (sc.getName().contains("AnnotationTest")){
				logger.debug("");
			}
			//Look at the class signature, see if it has any type parameters that need to be replaced 
			//in the methods
			String signatureTag = PostProcessor.getSignatureFromTag(sc.getTags());
			if (signatureTag != null){
				SignatureParser sp = SignatureParser.make();
				ClassSignature classSignature = sp.parseClassSig(signatureTag);
				FormalTypeParameter [] formalTypeParameters = classSignature.getFormalTypeParameters();
				for (FormalTypeParameter ftp : formalTypeParameters){
				}
				currentSuperTypeSignature = classSignature.getSuperclass();
				logger.debug("");

			}
			
			List<TypeAndTag> typeAndTags = new ArrayList<TypeAndTag>();
			
			//Collect all the fields if public
			for( SootField field : sc.getFields()){
				if (field.isPublic() && !field.isStatic() && !field.isFinal()){
					if (handler != null){
						handler.onFieldExtracdted(sc, field);
					}
					TypeAndTag typeAndTag = new TypeAndTag();
					typeAndTag.tags = field.getTags();
					typeAndTag.type = field.getType();
					typeAndTags.add(typeAndTag);
					
				}
			}
			
			
			//And for all the methods
			for (SootMethod method : sc.getMethods()){
				
				if (handler != null){
					handler.onMethodExtracted(sc, method);
				}
				TypeAndTag typeAndTag = new TypeAndTag();
				typeAndTag.type = method.getReturnType();
				typeAndTag.tags = method.getTags();
				typeAndTags.add(typeAndTag);
			}
			
			for (TypeAndTag typeAndTag : typeAndTags){
				
				Type retType = typeAndTag.type;
				List<Tag> tags = typeAndTag.tags;
				//A list is a ref type as well
				if (retType instanceof RefType){
					RefType refType = (RefType) retType;
					String retClassName = refType.getClassName();
										
					
					//The return type is a list so get the element types
					if (hasInterface(refType.getSootClass(), "java.util.List") || 
							hasInterface(refType.getSootClass(), "java.util.Collection") ) {
						String signature = PostProcessor.getSignatureFromTag(tags);
						List<String> genericClassNames = getClassNamesFromReturnSignature(signature);
						//Size should be 1
						if (genericClassNames.size() == 1){
							String className = genericClassNames.get(0);
							if (!classes.contains(className)){
								getMethodReturnClassNames(Scene.v().getSootClass(className), classes, superTypeSignature);
							}
						}

					} else if (hasInterface(refType.getSootClass(), "java.util.Map")) {
						String signature = PostProcessor.getSignatureFromTag(tags);
						//Size may not be 2 because we only return non framework types
						List<String> genericClassNames = getClassNamesFromReturnSignature(signature);
						for (String genericClassName : genericClassNames){
							if (!classes.contains(genericClassName)){
								getMethodReturnClassNames(Scene.v().getSootClass(genericClassName), classes, superTypeSignature);
							}
						}
						
					} else if (retClassName.equals("java.lang.Object")){
					//TODO this is where the generic type should be checked from the type parameter in class
						
					} else {
						if (!classes.contains(retClassName) && !PostProcessor.isFrameworkClass(retClassName)){
							
							if (currentSuperTypeSignature != null){
								getMethodReturnClassNames(Scene.v().getSootClass(retClassName), classes, currentSuperTypeSignature);
							} else {
								getMethodReturnClassNames(Scene.v().getSootClass(retClassName), classes, superTypeSignature);
							}
						}
					}
				}	
				
			}
			childSuperTypeSignature = currentSuperTypeSignature;
		}
	}
	
	

	/**
	 * 
	 * @param path The super class path. Path because of nesting classes.
	 * @return
	 */
	@SuppressWarnings("restriction")
	private List<String> getTypeByIdentifier(List<SimpleClassTypeSignature> path){
		List<String> classes = new ArrayList<String>();
		if (path == null){
			return classes;
		}
		
		//Seem to be in heiarchy order from parent up
		//TODO if super super how does this work?
		for (SimpleClassTypeSignature superClass : path){
			
			//Look at the type arguments for each super class
			for (TypeArgument typeArg : superClass.getTypeArguments()){
				if (typeArg instanceof ClassTypeSignature){
					List<String> foundTypeClasses = new ArrayList<String>();
					collectTypeParameterClassNames((ClassTypeSignature)typeArg, foundTypeClasses);
					classes.addAll(foundTypeClasses);
				}
			}
		}
		return classes;
	}
	
	/**
	 * Recursively handle the path
	 * @param cts
	 * @param path
	 */
	@SuppressWarnings("restriction")
	private void collectTypeParameterClassNames(ClassTypeSignature cts,  List<String> path){
		for (SimpleClassTypeSignature p : cts.getPath()){
			if (!PostProcessor.isFrameworkClass(p.getName())){
				path.add(p.getName());
			}
			//Look at the type arguments for each super class
			for (TypeArgument typeArg : p.getTypeArguments()){
				//typeArg.
				if (typeArg instanceof ClassTypeSignature){
					//((ClassTypeSignature)typeArg).getPath()
					collectTypeParameterClassNames((ClassTypeSignature) typeArg, path);
				}
			}
		}
	}
	
	/*
	private String parseClassNameFromAnnotation(String signatureTag){
	        Matcher matcher = pattern.matcher(signatureTag);
	        if (matcher.matches()){
	        	return matcher.group(1);
	        }
	        return null;
	}
	*/

	
	
	/**
	 * Just return a list of the super classnames excludes Object and including current class
	 * @return
	 */
	private List<String> getSuperClassNames(SootClass sootClass ){
		List<String> superClassNames = new ArrayList<String>();
		List<SootClass> superClasses = Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(sootClass);
		Iterator<SootClass> it = superClasses.iterator();
		while (it.hasNext()){
			SootClass sc = it.next();
			if (!sc.getName().equals("java.lang.Object")){
				superClassNames.add(sc.getName());
			}
		}
		return superClassNames;
	}


	/**
	 * Excludes Object, derived from unmodifiable list so create a new one without Object
	 * @return
	 */
	@SuppressWarnings("restriction")
	private List<SootClass> getSuperSootClasses(SootClass sootClass, boolean includeGenerics ){
		if (sootClass.isInterface()){
			return new ArrayList<SootClass>();
		}
		List<SootClass> superClasses = Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(sootClass);

		List<SootClass> superClassesWithoutObject = new ArrayList<SootClass>();
		for (int i=0; i< superClasses.size(); i++){
			SootClass sc = superClasses.get(i);
			if (!sc.getName().equals("java.lang.Object")){
				superClassesWithoutObject.add(sc);
			}
		}
		
		if (includeGenerics){
			//This is hacky but look at the generics for the extend and include them so 
			//iterate over
			String signatureTag = PostProcessor.getSignatureFromTag(sootClass.getTags());
			if (signatureTag != null){
				SignatureParser sp = SignatureParser.make();
				ClassSignature classSignature = sp.parseClassSig(signatureTag);
				FormalTypeParameter [] formalTypeParameters = classSignature.getFormalTypeParameters();
				for (FormalTypeParameter ftp : formalTypeParameters){
				}
				ClassTypeSignature currentSuperTypeSignature = classSignature.getSuperclass();
				logger.debug("");
				
				for (String _sc : getTypeByIdentifier(currentSuperTypeSignature.getPath()) ){
					superClassesWithoutObject.add(Scene.v().getSootClass(_sc));
				}
	
	
			}
			
		}
		return superClassesWithoutObject;
	}
	
	/**
	 * 
	 * @param sc
	 * @param classInterface
	 * @return
	 */
	private boolean hasInterface(SootClass sc, String classInterface){
		if (sc.isInterface()){
			return true;
		}
		for (SootClass s : getSuperSootClasses(sc, false)){
			//Need to look at all the supers too
			Iterator<SootClass> it = s.getInterfaces().iterator();
			while (it.hasNext()){
				SootClass i = it.next();
				if (i.getName().equals(classInterface)){
					return true;
				}
			}
		}
		return false;
	}
	
	@SuppressWarnings("restriction")
	public List<String> getClassTypeParameters(String sootSignatureTag){
		SignatureParser sp = SignatureParser.make();
		ClassSignature classSignature = sp.parseClassSig(sootSignatureTag);
		FormalTypeParameter [] formalTypeParameters = classSignature.getFormalTypeParameters();
		ClassTypeSignature currentSuperTypeSignature = classSignature.getSuperclass();
		return getTypeByIdentifier(currentSuperTypeSignature.getPath());
	}
	
	
	/**
	 * example: public T getObject()
	 * 
	 * @param sootSignatureTag
	 * @return
	 */
	@SuppressWarnings("restriction")
	public boolean isReturnATypeParameter(String sootSignatureTag){
		if (sootSignatureTag != null){
			
			//To force processes fields make it look like a method signature
			if (!sootSignatureTag.contains("(") && !sootSignatureTag.contains(")")){
				sootSignatureTag = "()" + sootSignatureTag;
			}
			SignatureParser sp = SignatureParser.make();
			MethodTypeSignature methodTypeSignature = sp.parseMethodSig(sootSignatureTag);
			ReturnType returnType = methodTypeSignature.getReturnType();
			if (returnType instanceof TypeVariableSignature){
				String identifier = ((TypeVariableSignature) returnType).getIdentifier();
				if (identifier != null){
					return true;
				}

			}
		}
		return false;
	}
	
	/**
	 * For ()Ljava/util/ArrayList<Lbr/com/i2mobile/rotalitoralpe/models/Estabelecimento;>;
	 * Return br.com.i2mobile.rotalitoralpe.models.Estabelecimento
	 * 
	 * @param listSignature
	 * @return
	 */
	@SuppressWarnings("restriction")
	public List<String> getClassNamesFromReturnSignature(String listSignature){
		//To force processes fields make it look like a method signature
		if (listSignature != null){
			if (!listSignature.contains("(") && !listSignature.contains(")")){
				listSignature = "()" + listSignature;
			}
			SignatureParser sp = SignatureParser.make();
			MethodTypeSignature methodTypeSignature = sp.parseMethodSig(listSignature);
			ReturnType returnType = methodTypeSignature.getReturnType();
			if (returnType instanceof ClassTypeSignature){
				
				List<String> argTypes = getTypeByIdentifier(((ClassTypeSignature) returnType).getPath());
				//For list there should only be one generic
				return argTypes;
			}
		}
		//PostProcessor.convertBytecodeToJavaClassName(classPath)
		return new ArrayList<String>();
	}
	
	
	class TypeAndTag {
		public Type type;
		public List<Tag> tags;
	}

}
