// Modified from web.engr.oregonstate.edu.zheng.gef.task.msexp.stacking.osu

package MikeExperiments;

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
			Var timestampedData, Var frequency, Var featurizeDataPath,
			Array windowSizes) throws Exception {
		logStep("Featurize data using the given window sizes");
		ExecutorBuilder featurize = rScript(
				featurizeFunctionScriptPath, callingScriptPath,
				var("featurizeCPD"), execConfig().setParallelizable(useCluster)
				.setNumJobs(clusterJobNum).setOnCluster(true)
				.setClusterWorkspace(clusterWorkspace));
		featurize.addParam("rawDataFilePath", String.class,
				timestampedData);
		featurize.addParam("frequency", Integer.class, frequency);
		featurize.addParam("savePath", String.class, featurizeDataPath);
		featurize.addParam("windowSize", Integer.class, var(windowSizes));
		featurize.addParam("hmm", String.class, var("true"));
		// add verification
		featurize.before(timestampedData);
		featurize.after(featurizeDataPath);
		// set mode and start
		featurize.prodMode();
		featurize.execute();
	}		

	private void splitData(String tvtDataAssignmentPath, Array splitId,
			int numSplits, Array fileNames) throws Exception {
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
				splitDataSetInto4Parts(fileNames, part1AssignmentTablePath,
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
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/free.living/data/merge.data.R");
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
				var("mergeData"),
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
				var("c(\"WindowId\",\"File\")"));
		createTrainData.prodMode();
		createTrainData.execute();
	}

	private void featurizeOSUData(String expRootPath, String datasetStr,
			String frequencyStr, String trialTimeFilePathStr,
			String rawDataPathStr, String rawDataExt,
			List<String> windowSizeList, List<String> trialGroupIdList,
			String tvtDataAssignmentPath, String tvtDataPath,
			String clusterWorkspace, Integer clusterJobNum, Boolean useCluster)
			throws Exception {
		Var dataset = var(datasetStr);
		Var frequency = var(frequencyStr);
		Var trialTimeFilePath = var(trialTimeFilePathStr);
		Var rawDataPath = var(rawDataPathStr);

		Array windowSizes = array(windowSizeList);
		Array trialGroupIds = array(trialGroupIdList);
		Var expPath = var(expRootPath).fileSep().cat(dataset).dot().cat("HMM").dot().cat(trialGroupIds);
		Var featurePath = expPath.fileSep().cat("features").fileSep().cat("ws")
				.cat(windowSizes);
		logStep("Convert the timestamped data to feature vectors");
		Table timeTrialTable = readTable(trialTimeFilePath);
		Array fileNames = column(timeTrialTable, "file");
		fileNames = which(
				fileNames,
				fileExists(rawDataPath.fileSep().cat(fileNames).cat(rawDataExt)));

		Var featurizeFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		Var timestampedData = rawDataPath.fileSep().cat(fileNames)
				.cat("PureTrial.csv");
		
		int numSplits = 30;
		Array splitId = array("[0:1:" + numSplits + "]");
		Array dataSets = array(Arrays.asList("trainBase", "validateBase", "trainHMM", "testHMM"));

		Var callingScriptPath = featurePath.fileSep().cat(fileNames)
				.cat("PureTrial.featurize.").cat("ws").cat(windowSizes)
				.cat(".R");
		Var featurizedFileExt = var("PureTrial.featurized.csv");
		Var featurizeDataPath = featurePath.fileSep().cat(fileNames)
				.cat(featurizedFileExt);

		String featurizedFileExtStr = "PureTrial.featurized.csv";

		// This is where the action happens
		featurize(clusterJobNum, useCluster, featurizeFunctionScriptPath, callingScriptPath, clusterWorkspace, timestampedData, frequency, featurizeDataPath, windowSizes);
		splitData(tvtDataAssignmentPath, splitId, numSplits, fileNames);
		merge(clusterJobNum, useCluster, dataSets, splitId, tvtDataPath, tvtDataAssignmentPath, clusterWorkspace, featurePath, featurizedFileExtStr, windowSizes);
	}

	private void splitDataSetInto4Parts(Array fileNames,
			Var part1AssignmentTablePath, Var part2AssignmentTablePath,
			Var part3AssignmentTablePath, Var part4AssignmentTablePath)
			throws Exception {
		Array labVisit1Files = which(fileNames, contains(fileNames, "V1"));
		log.info("Number of Visit1: " + labVisit1Files.getValues().size());
		List<Array> labVisit1Split = split(labVisit1Files, 4);

		Array labVisit2Files = subtract(fileNames, labVisit1Files);
		log.info("Number of Visit2: " + labVisit2Files.getValues().size());
		List<Array> labVisit2Split = split(labVisit2Files, 4);

		Array part1 = union(labVisit1Split.get(0), labVisit2Split.get(0));
		save(createTable(newColumn("SubjectID", part1)),
				part1AssignmentTablePath);

		Array part2 = union(labVisit1Split.get(1), labVisit2Split.get(1));
		save(createTable(newColumn("SubjectID", part2)),
				part2AssignmentTablePath);

		Array part3 = union(labVisit1Split.get(2), labVisit2Split.get(2));
		save(createTable(newColumn("SubjectID", part3)),
				part3AssignmentTablePath);

		Array part4 = union(labVisit1Split.get(3), labVisit2Split.get(3));
		save(createTable(newColumn("SubjectID", part4)),
				part4AssignmentTablePath);
	}

	private void OSU_YR4_Hip_30Hz_Hip() throws Exception {
		String expRootPath = "/nfs/guille/wong/users/andermic/Desktop/hmm";
		String datasetStr = "OSU_YR4_Hip_30Hz";
		String frequencyStr = "30";
		String trialTimeFilePathStr = "/nfs/guille/wong/wonglab2/obesity/2012/free.living/rawdata/30Hz/YR4/Cleaned Time Input GT3X Plus Hip 2_27_2012.csv";
		String rawDataPathStr = "/nfs/guille/wong/wonglab2/obesity/2012/free.living/rawdata/30Hz/YR4/converted.7cls";
		String rawDataExt = "PureTrial.csv";
		List<String> windowSizeList = Arrays.asList("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20");
		List<String> trialGroupIdList = Arrays.asList("7cls");
		String tvtDataPath = "/nfs/guille/wong/users/andermic/Desktop/hmm/OSU_YR4_Hip_30Hz.HMM.7cls";
		String tvtDataAssignmentPath = tvtDataPath + "/splits";
		
		String clusterWorkspace = "/nfs/guille/wong/users/andermic/Desktop/hmm/OSU_YR4_Hip_30Hz.HMM.7cls/cluster";
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		
		featurizeOSUData(expRootPath, datasetStr, frequencyStr,
				trialTimeFilePathStr, rawDataPathStr, rawDataExt,
				windowSizeList, trialGroupIdList, tvtDataAssignmentPath,
				tvtDataPath, clusterWorkspace,clusterJobNum, useCluster);
	}

	public static void main(String[] args) {
		try {
			new FeaturizeDataHMM().OSU_YR4_Hip_30Hz_Hip();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
