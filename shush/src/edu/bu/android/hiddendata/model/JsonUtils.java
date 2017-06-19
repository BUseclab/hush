package edu.bu.android.hiddendata.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class JsonUtils<T> {


	/**
	 * Load the parameter mappings
	 * @return
	 */
	public static DeserializeToUIConfig loadFirstPassResultFile(File file){
		DeserializeToUIConfig map = null;
		try {
			JsonReader reader = new JsonReader(new FileReader(file));
			Gson gson = new Gson();
			map = gson.fromJson(reader, DeserializeToUIConfig.class);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}
	public T load(File file, Class c ){
		T obj = null;
		try {
			JsonReader reader = new JsonReader(new FileReader(file));
			Gson gson = new Gson();
			obj = gson.fromJson(reader, c);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return obj;
	}
	public static void writeResults(File resultsFile, Object results){
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		PrintWriter writer = null;
		try {
				String json = gson.toJson(results);
			   writer = new PrintWriter(resultsFile, "UTF-8");
			   writer.write(json);
			  
			  } catch (IOException e) {
			   e.printStackTrace();
			  } finally{
				  if (writer != null){
					writer.close();
				  }
			  }
	}
	
}
