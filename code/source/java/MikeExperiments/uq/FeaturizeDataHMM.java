// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments.uq;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import web.engr.oregonstate.edu.zheng.gef.def.Array;
import web.engr.oregonstate.edu.zheng.gef.def.Table;
import web.engr.oregonstate.edu.zheng.gef.def.TaskDef;
import web.engr.oregonstate.edu.zheng.gef.def.Var;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder;
import web.engr.oregonstate.edu.zheng.gef.executor.ExecutorBuilder.VerificationType;

public class FeaturizeDataHMM extends TaskDef {
	private static final Logger log = Logger.getLogger(FeaturizeDataHMM.class
			.getName());

	protected FeaturizeDataHMM() {
	}

	private void featurize(Integer clusterJobNum,
			Boolean useCluster, Var featurizeFunctionScriptPath,
			Var callingScriptPath, String clusterWorkspace, 
			Var truncatedFileNames, Var duplicatesFileNames,
			Var eventsFileNames, Var frequency, Var featurizeDataPath,
			Array windowSizes, String day) throws Exception {
		logStep("Featurize data using the given window sizes");
		ExecutorBuilder featurize = rScript(
				featurizeFunctionScriptPath, callingScriptPath,
				var("featurizeUQCPD"), execConfig().setParallelizable(useCluster)
				.setNumJobs(clusterJobNum).setOnCluster(true)
				.setClusterWorkspace(clusterWorkspace));
		featurize.addParam("day", String.class, day);
		featurize.addParam("truncatedDataFilePath", String.class,
				truncatedFileNames);
		featurize.addParam("duplicatesDataFilePath", String.class,
				duplicatesFileNames);
		featurize.addParam("frequency", Integer.class, frequency);
		featurize.addParam("savePath", String.class, featurizeDataPath);
		featurize.addParam("eventsPath", String.class,
				eventsFileNames);
		featurize.addParam("windowSize", Integer.class, var(windowSizes));
		featurize.addParam("hmm", String.class, "true");
		// add verification
		featurize.before(truncatedFileNames);
		featurize.before(duplicatesFileNames);
		featurize.before(eventsFileNames);
		featurize.after(featurizeDataPath);
		// set mode and start
		featurize.prodMode();
		featurize.execute();
	}		

	private void splitData(String tvtDataAssignmentPath, Array splitId,
			int numSplits, Array subjectIDs) throws Exception {
		logStep("Split dataset into four subsets");
		Var dataSetAssignmentPath = var(tvtDataAssignmentPath);
		// Randomly assign training and validation data
		Array subsetId = array("[0:1:4]");
		Var assignmentTablePath = dataSetAssignmentPath.fileSep().cat("split")
				.cat(splitId).cat(".part").cat(subsetId).cat(".csv");
		if (!all(fileExists(assignmentTablePath))) {
			logStep("Randomly assign training, validating and testing data");
			for (int i = 0; i < numSplits; ++i) {
				Var part1AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part0.csv");
				Var part2AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part1.csv");
				Var part3AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part2.csv");
				Var part4AssignmentTablePath = dataSetAssignmentPath.fileSep()
						.cat("split").cat(String.valueOf(i)).cat(".part3.csv");
				splitDataSetInto4Parts(subjectIDs, part1AssignmentTablePath,
						part2AssignmentTablePath, part3AssignmentTablePath,
						part4AssignmentTablePath);
			}
		}
	}

	private void merge(Integer clusterJobNum, 
			Boolean useCluster, Array dataSets, Array splitId, 
			String tvtDataPath, String tvtDataAssignmentPath, 
			String clusterWorkspace, Var featurePath,
			String featurizedFileExtStr, Array windowSizes)
			throws Exception {
		logStep("Merge featurized data to train1, validate, train2, and test data set files");
		Array dataSetFileExtension = array(Arrays.asList(featurizedFileExtStr,
				featurizedFileExtStr, featurizedFileExtStr, featurizedFileExtStr));
		Array assignment = array(Arrays.asList("0", "1", "2", "3"));
		bind(dataSets, dataSetFileExtension, assignment);
	
		Var iterationId = var("split").cat(splitId);
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var trainValidateTestDataPath = var(tvtDataPath);
		Var mergeTrainDataCallingPath = trainValidateTestDataPath.fileSep()
				.cat(iterationId).fileSep().cat("merge.").cat(dataSets)
				.dot().cat("ws").cat(windowSizes).cat(".data.R");
		Var saveDataPath = trainValidateTestDataPath.fileSep().cat(iterationId)
				.fileSep().cat(dataSets).cat(".ws").cat(windowSizes)
				.cat(".data.csv");
		Var dataAssignmentTablePath = var(tvtDataAssignmentPath).fileSep()
				.cat("split").cat(splitId).cat(".part").cat(assignment)
				.cat(".csv");
		ExecutorBuilder createTrainData = rScript(
				mergeDataFunctionScriptPath,
				mergeTrainDataCallingPath,
				var("mergeDataUQ"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		createTrainData.addParam("dataFileFolder", String.class, featurePath);
		createTrainData.addParam("dataNameFilePath", String.class,
				dataAssignmentTablePath, VerificationType.Before);
		createTrainData.addParam("dataFileExtension", String.class,
				var(dataSetFileExtension));
		createTrainData.addParam("savePath", String.class, saveDataPath,
				VerificationType.After);
		createTrainData.addParam("columns", List.class,
				var("c(\"WindowId\",\"SubjectID\")"));
		createTrainData.prodMode();
		createTrainData.execute();
	}

	private void featurizeUQData(String expRootPath, String datasetStr,
			String frequencyStr, String rawDataPathStr, List<String> windowSizeList,
			String tvtDataAssignmentPath, String tvtDataPath,
			String clusterWorkspace, Integer clusterJobNum, Boolean useCluster,
			Array subjectIDs, String day, int numSplits, Array splitId)
			throws Exception {
		Var dataset = var(datasetStr);
		Var frequency = var(frequencyStr);
		Var rawDataPath = var(rawDataPathStr);

		Array windowSizes = array(windowSizeList);
		Var expPath = var(expRootPath).fileSep().cat(dataset).cat(".HMM");
		Var featurePath = expPath.fileSep().cat("features").fileSep().cat("ws")
				.cat(windowSizes);
		logStep("Convert the timestamped data to feature vectors");
		
		/*Table timeTrialTable = readTable(trialTimeFilePath);
		Array fileNames = column(timeTrialTable, "file");
		fileNames = which(
				fileNames,
				fileExists(rawDataPath.fileSep().cat(fileNames).cat(rawDataExt)));*/
		Var truncatedFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_truncated_day").cat(day).cat(".csv");
		Var duplicatesFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_duplicates_day").cat(day).cat(".csv");
		//Var startEndFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_start_and_end.csv");
		Var eventsFileNames = rawDataPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_events_day").cat(day).cat(".csv");
		Var callingScriptPath = featurePath.fileSep().cat(subjectIDs).cat(".featurize.R");
		Var featurizeDataPath = featurePath.fileSep().cat(subjectIDs).cat(".featurized.csv");

		Var featurizeFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		mkdir(featurePath);
		
		Array dataSets = array(Arrays.asList("trainBase", "validateBase", "trainHMM", "testHMM"));

		String featurizedFileExtStr = ".featurized.csv";

		// This is where the action happens
		featurize(clusterJobNum, useCluster, featurizeFunctionScriptPath, callingScriptPath, clusterWorkspace, truncatedFileNames, duplicatesFileNames, eventsFileNames, frequency, featurizeDataPath, windowSizes, day);
		splitData(tvtDataAssignmentPath, splitId, numSplits, subjectIDs);
		merge(clusterJobNum, useCluster, dataSets, splitId, tvtDataPath, tvtDataAssignmentPath, clusterWorkspace, featurePath, featurizedFileExtStr, windowSizes);
	}

	private void splitDataSetInto4Parts(Array subjectIDs,
			Var part1AssignmentTablePath, Var part2AssignmentTablePath,
			Var part3AssignmentTablePath, Var part4AssignmentTablePath)
			throws Exception {
		List<Array> splits = split(subjectIDs, 4);
		
		Array part1 = splits.get(0);
		save(createTable(newColumn("SubjectID", part1)),
				part1AssignmentTablePath);

		Array part2 = splits.get(1);
		save(createTable(newColumn("SubjectID", part2)),
				part2AssignmentTablePath);

		Array part3 = splits.get(2);
		save(createTable(newColumn("SubjectID", part3)),
				part3AssignmentTablePath);

		Array part4 = splits.get(3);
		save(createTable(newColumn("SubjectID", part4)),
				part4AssignmentTablePath);
	}

	private void UQ_30Hz() throws Exception {
		String expRootPath = "/nfs/guille/wong/wonglab3/obesity/2012/hmm";
		String day = "2";
		String frequencyStr = "30";
		String datasetStr = "uq_" + frequencyStr + "Hz_day" + day;

		String rawDataPathStr = "/nfs/guille/wong/users/andermic/uq/processed";
		List<String> windowSizeList = Arrays.asList("10","12","14","16","18","20");
		windowSizeList = Arrays.asList("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20");
		windowSizeList = Arrays.asList("20");
		String tvtDataPath = expRootPath + "/" + datasetStr + ".HMM";
		String tvtDataAssignmentPath = tvtDataPath + "/splits";
		
		String clusterWorkspace = tvtDataPath + "/cluster";
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		
		Array subjectIDs = array(Arrays.asList("1", "2", "3", "4", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "19", "20", "21", "22", "23", "24", "25"));
		int numSplits = 30;
		Array splitId = array("[0:1:" + numSplits + "]");
		
		featurizeUQData(expRootPath, datasetStr, frequencyStr,
				rawDataPathStr, windowSizeList, tvtDataAssignmentPath,
				tvtDataPath, clusterWorkspace, clusterJobNum, useCluster,
				subjectIDs, day, numSplits, splitId);
	}

	public static void main(String[] args) {
		try {
			new FeaturizeDataHMM().UQ_30Hz();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
