package edu.bu.android.hiddendata.obfuscation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.bu.android.hiddendata.ConfigUtils;

/**
 * Processes obfuscation results to create source and sink file for obfuscated signatures
 * @author Wil Koch
 *
 */
public class ObfuscatedSource {
	private static final Logger logger = LoggerFactory.getLogger(ObfuscatedSource.class.getName());
	Set<String> sinks = new HashSet<String>();
	File obfuscatedSignatureDir;
	
	public ObfuscatedSource(String directoryPath){
		this.obfuscatedSignatureDir = new File(directoryPath);

		if (!obfuscatedSignatureDir.exists()){
			System.err.println("Directory " + obfuscatedSignatureDir.getAbsolutePath() + " doesnt exist");
			System.exit(1);
		}
	}
	public void readSources(){
		for (File f : obfuscatedSignatureDir.listFiles()){
			readSource(f);
		}
		logger.info("Size: {}",sinks.size());
		
	}
	
	
	public void writeSourceSinkFile(File sourceSinkFile){
		ConfigUtils.createSinkSourceFile("SourcesAndSinks_1.txt", sourceSinkFile,  new HashSet<String>(), sinks);
	}
	
	private void readSource(File f){
		try {
			for (String line : Files.readAllLines(Paths.get(f.getAbsolutePath()), Charset.defaultCharset())){
				sinks.add(line);
				logger.info(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		/**
		 * path: Path of where to find the directory containing the files with the jsonJson
		 */
		String path = args[0];
		String output = args[1];
		ObfuscatedSource o = new ObfuscatedSource(path);
		o.readSources();
		o.writeSourceSinkFile(new File(output));
	}
}
