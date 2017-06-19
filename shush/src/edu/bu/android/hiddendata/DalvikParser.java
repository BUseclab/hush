package edu.bu.android.hiddendata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * <Class Generics>Extended class
 * @see 4.3.4 Signatures https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html
 * @See https://source.android.com/devices/tech/dalvik/dex-format.html
 * @author wil
 *
 */
@Deprecated
public class DalvikParser {

	/*
	 * (\\<[\\w\\:\\;/]+\\>)? The optional generic for the class
	 * ([VZBSCIJFDL\\[][\\w/]+(\\<[\\w\\:\\;/\\<\\>]+\\>)?)\\; The required
	 * extended class (([VZBSCIJFDL\\[][\\w/]+(\\<[\\w\\:\\;/\\<\\>]+\\>)?)\\;)?
	 * The optional interface
	 */
	private static final String PATTERN_CLASS_SIGNATURE = "^(\\<[\\w\\:\\;/]+\\>)?([VZBSCIJFDL\\[])([\\w/]+)(\\<[\\w\\:\\;/\\<\\>]+\\>)?\\;(([VZBSCIJFDL\\[])([\\w/]+)(\\<[\\w\\:\\;/\\<\\>]+\\>)?\\;)?$";
	private static final String PATTERN_METHOD_SIGNATURE = "([VZBSCIJFDL\\[])([\\w/]+)";
	private static final String PATTERN_CLASS_TYPE_PARAMTERS = "(\\w)\\:([VZBSCIJFDL\\[])([\\w/]+)";
	private Pattern pattern = Pattern.compile(PATTERN_METHOD_SIGNATURE);
	private Pattern patternClassTypeParameters = Pattern.compile(PATTERN_CLASS_TYPE_PARAMTERS);


	private Map<String, String>  parseClassTypeParameters(String classTypeParameters){
		Map<String, String> nameToTypeMapping = new HashMap<String,String>();
		Matcher matcher = patternClassTypeParameters.matcher(classTypeParameters);
		while (matcher.find()){
			String name = matcher.group(1);
			char type = matcher.group(2).toCharArray()[0];
			String className = matcher.group(3);
			nameToTypeMapping.put(name, className);
		}
		return nameToTypeMapping;
	}

	public class DalvikClassSignature {
		private char type;
		private String className;
		private Map<String, String> nameToTypeMapping;
		
		
	}

}
