package edu.bu.android.hiddendata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import edu.bu.android.hiddendata.infoflow.CallGraphAndroidPatcher;

public class FindHidden {
	
	public enum Mode {
		NETWORK_TO_DESERIALIZE,
		DESERIALIZE_TO_UI,
		LIST_ANALYSIS
	}
	
	private static final Logger logger = LoggerFactory.getLogger(FindHidden.class.getName());
	static String command;
	static boolean generate = false;
	
	private static int timeout = -1;
	private static int sysTimeout = -1;
	
	public static final String JAVA_ERROR_PREFIX = "java_error";
	public static final String FLAG_MODEL = "net2model.flag";
	public static final String FLAG_DONE = "model2ui.flag";
	protected static final String RESULT_DIRECTORY = "results";
	protected static final String SOURCESINK_SUFFEX = "-model2ui_sources_sinks.txt";
	protected static final String EASY_TAINT_WRAPPER_FILE_PREFIX = "-easytaintwrapper.txt";
	protected static final String MODEL_TO_UI_CONFIG_SUFFIX = "-model2ui_config.json";
	protected static final String LIST_SUFFIX = "-list_sources_sinks.txt";
	protected static final String RESULTS_SUFFIX = "-results.json";

	public static String EXEC_ID;
	private static boolean forceRun = false;
	private static String resultsDirectory = RESULT_DIRECTORY;
	private static String injectionsFilePath = null;
	private static String sourcesAndSinksFilePath = "SourcesAndSinks.txt";
	private static String easyTaintFilePath = null;
	private static boolean stopAfterFirstFlow = false;
	private static boolean implicitFlows = false;
	private static boolean staticTracking = true;
	private static boolean enableCallbacks = true;
	private static boolean enableExceptions = true;
	private static int accessPathLength = 5;
	private static LayoutMatchingMode layoutMatchingMode = LayoutMatchingMode.MatchSensitiveOnly;
	private static boolean flowSensitiveAliasing = true;
	private static boolean computeResultPaths = true;
	private static boolean aggressiveTaintWrapper = false;
	private static boolean librarySummaryTaintWrapper = false;
	private static String summaryPath = "";
	private static PathBuilder pathBuilder = PathBuilder.ContextInsensitiveSourceFinder;
	private static boolean useFragments = false;

	private static CallgraphAlgorithm callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
	
	private static boolean DEBUG = false;
	
	
	private static Mode mode;


	
	/**
	 * @param args Program arguments. args[0] = path to apk-file,
	 * args[1] = path to android-dir (path/android-platforms/)
	 */
	public static void main(final String[] args) throws IOException, InterruptedException {
		EXEC_ID = System.currentTimeMillis() + "";
		
		if (args.length < 2) {
			printUsage();	
			return;
		}
		//start with cleanup:
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()){
			boolean success = true;
			for(File f : outputDir.listFiles()){
				success = success && f.delete();
			}
			if(!success){
				System.err.println("Cleanup of output directory "+ outputDir + " failed!");
			}
			outputDir.delete();
		}
		
		// Parse additional command-line arguments
		if (!parseAdditionalOptions(args))
			return;
		if (!validateAdditionalOptions())
			return;
		
		List<String> apkFiles = new ArrayList<String>();
		File apkFile = new File(args[0]);
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {
			
				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}
			
			});
			for (String s : dirFiles) {
				
				apkFiles.add(new File(args[0], s).getAbsolutePath());
			}
		} else {
			String extension = apkFile.getName().substring(apkFile.getName().lastIndexOf("."));

			if (extension.equalsIgnoreCase(".txt")) {
				BufferedReader rdr = new BufferedReader(new FileReader(apkFile));
				String line = null;
				while ((line = rdr.readLine()) != null)
					apkFiles.add(line);
				rdr.close();
			}
			else if (extension.equalsIgnoreCase(".apk"))
				apkFiles.add(args[0]);
			else {
				System.err.println("Invalid input file format: " + extension);
				return;
			}
		}
		int apkCount = 0;
		for (final String fullFilePath : apkFiles) {
			//final String fullFilePath;
			apkCount++;
			double percentDone = (apkCount * 1.0) / (apkFiles.size() * 1.0) * 100;
			String sPercentDone = String.format("%.2f",percentDone); //88%
			logger.info(sPercentDone + "% (" + apkCount + " of " + apkFiles.size() + ") Analyzing file " + fullFilePath + "...");
				
			

			//Set the source sink file to be used
			String apkFileName = new File(fullFilePath).getName();
			String sourceAndSinkFileName = apkFileName + SOURCESINK_SUFFEX;
			String easyTaintFileName = apkFileName + EASY_TAINT_WRAPPER_FILE_PREFIX;

			File apkResult1Dir = new File(resultsDirectory, apkFileName);
			
			//If the file already exists then do the second pass
			if (!apkResult1Dir.exists()){
				apkResult1Dir.mkdirs();
			} 
			
			File netToModelFlagFile = new File(apkResult1Dir, FLAG_MODEL );
			File listFlagFile = new File(apkResult1Dir, "list.flag");
			File modelToUIFlagFile = new File(apkResult1Dir, FLAG_DONE);
			
			logger.info("Path: " + fullFilePath + " arg1: " + args[1]);

			
			//File pass3FlagFile = new File(apkResult1Dir, ".pass3");
			
			mode = Mode.NETWORK_TO_DESERIALIZE;

			//Already run? Skip
			if (modelToUIFlagFile.exists()){
				logger.warn("Already ran analysis, skipping");
				continue;
			}
			
			//Whats the current mode?
			if (netToModelFlagFile.exists()){
				mode = Mode.LIST_ANALYSIS;
				if (listFlagFile.exists()){
					mode = Mode.DESERIALIZE_TO_UI;
				}
			}
			
			//check for a previous out of memory  java_error file, skip if present unless we are forcing it to execute
			if (hasJavaError(apkResult1Dir) && !forceRun){
				logger.warn("Previous analysis ran out of memory, skipping");
				continue;
			}
			
			//If we have a sys timeout then we need to spawn 
			//a new processes and it will restart this for a single app
			if (sysTimeout > 0) {
				runAnalysisSysTimeout(apkResult1Dir, fullFilePath, args[1]);
				continue;
			}
			
			


			
			AnalysisResults results = null;
			
			long stamp = System.currentTimeMillis();
			
			File sourceSinkFileList = new File(apkResult1Dir, apkFileName + LIST_SUFFIX);
			File resultsFile = new File(apkResult1Dir, apkFileName + "-" + stamp + RESULTS_SUFFIX);
			File sourceSinkFileDeserializeToUI = new File(apkResult1Dir, sourceAndSinkFileName);
			File easyTaintFileDeserializeToUI  = new File(apkResult1Dir, easyTaintFileName );
			File jsonToUIresultsFile = new File(apkResult1Dir, apkFileName + FindHidden.MODEL_TO_UI_CONFIG_SUFFIX );

			//TODO we should check existance of flag files here
			final long beforeRun = System.nanoTime();
			boolean shouldProcessLists = true;
			switch (mode){
				case NETWORK_TO_DESERIALIZE: {
					
					logger.info("=========================================");
					logger.info("Pass 1: Network to deserializer");
					logger.info("=========================================");

					//Keep the source sink and easy taint default
					//Infoflow.setPathAgnosticResults(true);

					logger.info("Using " + sourcesAndSinksFilePath + " as source-sink file.");

					// Run the analysis using defaults 
					//results = run(fullFilePath, args[1]);
					results = runAnalysis(fullFilePath, args[1]);
					
					if (results.infoFlowResults == null || results.infoFlowResults.isEmpty()){
						logger.error("Could not find any flows from Network to Deserialize, cannot continue");
						//Create this so we skip it if run again
						modelToUIFlagFile.createNewFile();
						return;
					}
					
					//Create source sink for next pass as well
					NetworkToDeserializePostProcessor pass1 = new NetworkToDeserializePostProcessor(results.context,  sourceSinkFileDeserializeToUI, sourceSinkFileList, easyTaintFileDeserializeToUI, jsonToUIresultsFile, results.infoFlowResults);
					shouldProcessLists = pass1.process();
					if (!shouldProcessLists){
						listFlagFile.createNewFile(); //if we crash here dont restart at list
					}

					System.gc();

					//Only do an additional pass if there were some instances where the model classes were added to lists
					//if (!pass1.getModelToAddSignatureMapping().isEmpty()){
						
			
					//} else {
					//	logger.warn("No models were found being added to lists");
					//}
						
					netToModelFlagFile.createNewFile();

				}
				case LIST_ANALYSIS: {
					logger.info("=========================================");
					logger.info("Pass 2: Lists to List.add");
					logger.info("=========================================");

					if (shouldProcessLists){
						
						//Note: I dont believe this needs agnositic should be false because
						//we only need to location the source here for the injection
						sourcesAndSinksFilePath = sourceSinkFileList.getAbsolutePath();
						//easyTaintFilePath = easyTaintFileDeserializeToUI.getAbsolutePath();
	
						results = runAnalysis(fullFilePath, args[1]);
						ListFlowsPostProcessor la = new ListFlowsPostProcessor(results.context, results.infoFlowResults, jsonToUIresultsFile, sourceSinkFileDeserializeToUI);
						la.process();
						System.gc();
					} else {
						logger.warn("No source and sinks found");
					}
					listFlagFile.createNewFile();
				}
				case DESERIALIZE_TO_UI: {
					logger.info("=========================================");
					logger.info("Pass 3: Deserializer to UI");
					logger.info("=========================================");


					//Infoflow.setPathAgnosticResults(false);
					injectionsFilePath = jsonToUIresultsFile.getAbsolutePath();
					
					sourcesAndSinksFilePath = sourceSinkFileDeserializeToUI.getAbsolutePath();
					//easyTaintFilePath = easyTaintFileDeserializeToUI.getAbsolutePath();

					logger.info("Using " + sourcesAndSinksFilePath + " as source-sink file.");

					// Run the analysis
					results = runAnalysis(fullFilePath, args[1]);
					
					//Now we are done so we need to figure out where we didnt have mappings
					new DeserializeToUiPostProcessor(resultsFile, jsonToUIresultsFile, results.context, results.infoFlowResults).findHiddenData();
						
					modelToUIFlagFile.createNewFile();

				}
			}

			System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");

			System.gc();

			
		} //end for 
	}

	private static boolean hasJavaError(File dir){
		String [] resultFile = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.startsWith(JAVA_ERROR_PREFIX));
			}
		});
		return resultFile.length > 0;
	}

	private static void runAnalysisSysTimeout(File apkResultDir, final String fileName, final String androidJar) {
			
			RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
			List<String> jvmArgs = runtimeMxBean.getInputArguments();
			
	
			String classpath = System.getProperty("java.class.path");
			String javaHome = System.getProperty("java.home");
			String executable = "/usr/bin/timeout";
			
	//TODO pull request, this was set to timeout in minutes, docs say seconds
			
			List<String> cmd = new ArrayList<String>();
			
			//The timeout executable
			cmd.add(executable);
			cmd.add("-s");
			cmd.add( "KILL");
			cmd.add(sysTimeout + "s");
	
			//The executable to run
			cmd.add(javaHome + "/bin/java");
			
			//The jvm arguments, same as we started
			cmd.addAll(jvmArgs);
			
			//If the JVM crashes a hs_error_pid.log file will be created, place this file in the 
			//directory of the analysis running
			File jvmLogFile = new File(apkResultDir, JAVA_ERROR_PREFIX + "%p.log");
			cmd.add("-XX:ErrorFile=" + jvmLogFile.getAbsolutePath());
			
			cmd.add("-cp");
			cmd.add(classpath);
			cmd.add("edu.bu.android.hiddendata.FindHidden");
			
			//The arguments
			cmd.add(fileName);
			cmd.add(androidJar);
			
			//Flags
			cmd.add(stopAfterFirstFlow ? "--singleflow" : "--nosingleflow");
			cmd.add(useFragments ? "--fragments" : "--nofragments");
			cmd.add(implicitFlows ? "--implicit" : "--noimplicit");
			cmd.add(staticTracking ? "--static" : "--nostatic");
			cmd.add(flowSensitiveAliasing ? "--aliasflowsens" : "--aliasflowins");
			cmd.add(computeResultPaths ? "--paths" : "--nopaths");
			cmd.add(aggressiveTaintWrapper ? "--aggressivetw" : "--nonaggressivetw");
			cmd.add(enableCallbacks ? "--callbacks" : "--nocallbacks");
			cmd.add(enableExceptions ? "--exceptions" : "--noexceptions");
			
			if (forceRun){
				cmd.add("--force");
			}
			
			cmd.add("--aplength");
			cmd.add(Integer.toString(accessPathLength));
			
			cmd.add("--cgalgo");
			cmd.add(callgraphAlgorithmToString(callgraphAlgorithm));
	
			cmd.add("--layoutmode");
			cmd.add(layoutMatchingModeToString(layoutMatchingMode));
			
			cmd.add("--pathalgo");
			cmd.add(pathAlgorithmToString(pathBuilder));
			
			cmd.add("--SOURCESSINKS");
			cmd.add(sourcesAndSinksFilePath);
			
			if (easyTaintFilePath != null){
				cmd.add("--EASYTAINT");
				cmd.add(easyTaintFilePath);
			}
			
			cmd.add("--output");
			cmd.add(resultsDirectory);
	
			logger.info("Running command: {}", cmd);
			
			
			String[] commandArray = new String[cmd.size()];
			commandArray = cmd.toArray(commandArray);
			
			try {
				ProcessBuilder pb = new ProcessBuilder(commandArray);
				//pb.inheritIO();
				
				//All of the proper logs are redirected to error
				
				File f= new File(apkResultDir, new File(fileName).getName() + "-" + EXEC_ID + ".log");
				//pb.redirectOutput(f);
				pb.redirectError(f);//new File("err_" + new File(fileName).getName() + ".txt"));
				Process proc = pb.start();
			
				proc.waitFor();
			} catch (IOException ex) {
				System.err.println("Could not execute timeout command: " + ex.getMessage());
				ex.printStackTrace();
			} catch (InterruptedException ex) {
				System.err.println("Process was interrupted: " + ex.getMessage());
				ex.printStackTrace();
			}
		}


	private static AnalysisResults runAnalysis(final String fileName, final String androidJar) {
		try {
	
			final SetupApplication	app = new SetupApplication(androidJar, fileName);
	
			app.setStopAfterFirstFlow(stopAfterFirstFlow);
			app.setEnableImplicitFlows(implicitFlows);
			app.setEnableStaticFieldTracking(staticTracking);
			app.setEnableCallbacks(enableCallbacks);
			app.setEnableExceptionTracking(enableExceptions);
			app.setAccessPathLength(accessPathLength);
			app.setLayoutMatchingMode(layoutMatchingMode);
			app.setFlowSensitiveAliasing(flowSensitiveAliasing);
			app.setPathBuilder(pathBuilder);
			app.setComputeResultPaths(computeResultPaths);
			app.setUseFragments(useFragments);
			app.setInjectionsFilePath(injectionsFilePath);
			final ITaintPropagationWrapper taintWrapper;
	
			//Create a new file from this string paramter
			final EasyTaintWrapper easyTaintWrapper;
			if (easyTaintFilePath != null && new File(easyTaintFilePath).exists())
				easyTaintWrapper = new EasyTaintWrapper(easyTaintFilePath);
			else if (new File("../soot-infoflow/EasyTaintWrapperSource.txt").exists())
				easyTaintWrapper = new EasyTaintWrapper("../soot-infoflow/EasyTaintWrapperSource.txt");
			else
				easyTaintWrapper = new EasyTaintWrapper("EasyTaintWrapperSource.txt");
			easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
			taintWrapper = easyTaintWrapper;
		
			
			app.addPreprocessor(new CallGraphAndroidPatcher(injectionsFilePath));
			
			
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypoints(sourcesAndSinksFilePath);
			//app.calculateSourcesSinksEntrypoints("SuSiExport.xml");
			
			
			if (DEBUG) {
				app.printEntrypoints();
				app.printSinks();
				app.printSources();
				
				app.addPreprocessor(new DebugHelper());
			}
				
			System.out.println("Running data flow analysis...");
			final InfoflowResults res = app.runInfoflow();
			
			AnalysisResults r = new AnalysisResults();
			r.context = app;
			r.infoFlowResults = res;
			return r;
			
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}


	private static boolean parseAdditionalOptions(String[] args) {
		int i = 2;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("--timeout")) {
				timeout = Integer.valueOf(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--systimeout")) {
				sysTimeout = Integer.valueOf(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--singleflow")) {
				stopAfterFirstFlow = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--implicit")) {
				implicitFlows = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--nostatic")) {
				staticTracking = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--aplength")) {
				accessPathLength = Integer.valueOf(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--cgalgo")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("AUTO"))
					callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
				else if (algo.equalsIgnoreCase("CHA"))
					callgraphAlgorithm = CallgraphAlgorithm.CHA;
				else if (algo.equalsIgnoreCase("VTA"))
					callgraphAlgorithm = CallgraphAlgorithm.VTA;
				else if (algo.equalsIgnoreCase("RTA"))
					callgraphAlgorithm = CallgraphAlgorithm.RTA;
				else if (algo.equalsIgnoreCase("SPARK"))
					callgraphAlgorithm = CallgraphAlgorithm.SPARK;
				else {
					System.err.println("Invalid callgraph algorithm");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--nocallbacks")) {
				enableCallbacks = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--noexceptions")) {
				enableExceptions = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--layoutmode")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("NONE"))
					layoutMatchingMode = LayoutMatchingMode.NoMatch;
				else if (algo.equalsIgnoreCase("PWD"))
					layoutMatchingMode = LayoutMatchingMode.MatchSensitiveOnly;
				else if (algo.equalsIgnoreCase("ALL"))
					layoutMatchingMode = LayoutMatchingMode.MatchAll;
				else {
					System.err.println("Invalid layout matching mode");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--aliasflowins")) {
				flowSensitiveAliasing = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--nopaths")) {
				computeResultPaths = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--force")) {
				forceRun = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--aggressivetw")) {
//TODO Pull request, this could never be true before
				aggressiveTaintWrapper = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--pathalgo")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("CONTEXTSENSITIVE"))
					pathBuilder = PathBuilder.ContextSensitive;
				else if (algo.equalsIgnoreCase("CONTEXTINSENSITIVE"))
					pathBuilder = PathBuilder.ContextInsensitive;
				else if (algo.equalsIgnoreCase("SOURCESONLY"))
					pathBuilder = PathBuilder.ContextInsensitiveSourceFinder;
				else {
					System.err.println("Invalid path reconstruction algorithm");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--libsumtw")) {
				librarySummaryTaintWrapper = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--summarypath")) {
				summaryPath = args[i + 1];
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--fragments")){
				useFragments = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--sourcessinks")){
				sourcesAndSinksFilePath = args[i + 1];
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--easytaint")){
				easyTaintFilePath = args[i + 1];
				i += 2;
			} 
			else if (args[i].equalsIgnoreCase("--output")){
				resultsDirectory = args[i + 1];
				i += 2;
			}else
				i++;
		}
		return true;
	}
	
	private static boolean validateAdditionalOptions() {
		if (timeout > 0 && sysTimeout > 0) {
			return false;
		}
		if (!flowSensitiveAliasing && callgraphAlgorithm != CallgraphAlgorithm.OnDemand
				&& callgraphAlgorithm != CallgraphAlgorithm.AutomaticSelection) {
			System.err.println("Flow-insensitive aliasing can only be configured for callgraph "
					+ "algorithms that support this choice.");
			return false;
		}
		if (librarySummaryTaintWrapper && summaryPath.isEmpty()) {
			System.err.println("Summary path must be specified when using library summaries");
			return false;
		}
		return true;
	}

	private static String callgraphAlgorithmToString(CallgraphAlgorithm algorihm) {
		switch (algorihm) {
			case AutomaticSelection:
				return "AUTO";
			case CHA:
				return "CHA";
			case VTA:
				return "VTA";
			case RTA:
				return "RTA";
			case SPARK:
				return "SPARK";
			default:
				return "unknown";
		}
	}

	private static String layoutMatchingModeToString(LayoutMatchingMode mode) {
		switch (mode) {
			case NoMatch:
				return "NONE";
			case MatchSensitiveOnly:
				return "PWD";
			case MatchAll:
				return "ALL";
			default:
				return "unknown";
		}
	}
	
	private static String pathAlgorithmToString(PathBuilder pathBuilder) {
		switch (pathBuilder) {
			case ContextSensitive:
				return "CONTEXTSENSITIVE";
			case ContextInsensitive :
				return "CONTEXTINSENSITIVE";
			case ContextInsensitiveSourceFinder :
				return "SOURCESONLY";
			default :
				return "UNKNOWN";
		}
	}
	
	
	//TODO need a proper parsing library for this
	private static void printUsage() {
		System.out.println("FlowDroid (c) Secure Software Engineering Group @ EC SPRIDE");
		System.out.println();
		System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");
		System.out.println("Optional further parameters:");
		System.out.println("\t--TIMEOUT n Time out after n seconds");
		System.out.println("\t--SYSTIMEOUT n Hard time out (kill process) after n seconds, Unix only");
		System.out.println("\t--SINGLEFLOW Stop after finding first leak");
		System.out.println("\t--IMPLICIT Enable implicit flows");
		System.out.println("\t--NOSTATIC Disable static field tracking");
		System.out.println("\t--NOEXCEPTIONS Disable exception tracking");
		System.out.println("\t--APLENGTH n Set access path length to n");
		System.out.println("\t--CGALGO x Use callgraph algorithm x");
		System.out.println("\t--NOCALLBACKS Disable callback analysis");
		System.out.println("\t--LAYOUTMODE x Set UI control analysis mode to x");
		System.out.println("\t--ALIASFLOWINS Use a flow insensitive alias search");
		System.out.println("\t--NOPATHS Do not compute result paths");
		System.out.println("\t--AGGRESSIVETW Use taint wrapper in aggressive mode");
		System.out.println("\t--PATHALGO Use path reconstruction algorithm x");
		System.out.println("\t--FRAGMENTS Enable use of Fragments, not enabled by default");
		System.out.println("\t--SOURCESSINKS Full path of SourcesAndSinks.txt");
		System.out.println("\t--EASYTAINT Full path of easy taint wrapper file.");
		System.out.println("\t--OUTPUT (Optional) Output directory");
		System.out.println("\t--FORCE (Optional) Force re-run");

		System.out.println();
		System.out.println("Supported callgraph algorithms: AUTO, CHA, RTA, VTA, SPARK");
		System.out.println("Supported layout mode algorithms: NONE, PWD, ALL");
		System.out.println("Supported path algorithms: CONTEXTSENSITIVE, CONTEXTINSENSITIVE, SOURCESONLY");
	}

	private static final class AnalysisResults {
		InfoflowResults infoFlowResults;
		SetupApplication context;
	}
	
	
	
}
