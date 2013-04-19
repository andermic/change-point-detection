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

public class FeaturizeDataCPD extends TaskDef {
	private static final Logger log = Logger.getLogger(FeaturizeDataCPD.class
			.getName());

	protected FeaturizeDataCPD() {
	}

	private void featurizeGroundTruth(Var expPath, Integer clusterJobNum,
			Boolean useCluster, Var featurizeFunctionScriptPath,
			Var callingScriptPath, String clusterWorkspace, 
			Var truncatedFileNames, Var duplicatesFileNames,
			Var eventsFileNames, Var frequency, Var featurizeDataPath,
			Var startEndFileNames) throws Exception {
		logStep("Featurize data using ground truth windows");
		ExecutorBuilder featurizeGroundTruth = rScript(
				featurizeFunctionScriptPath,
				callingScriptPath,
				var("featurizeUQCPD"),
				execConfig().setParallelizable(useCluster).setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		featurizeGroundTruth.addParam("truncatedDataFilePath", String.class,
				truncatedFileNames);
		featurizeGroundTruth.addParam("duplicatesDataFilePath", String.class,
				duplicatesFileNames);
		featurizeGroundTruth.addParam("eventsPath", String.class,
				eventsFileNames);
		featurizeGroundTruth.addParam("savePath", String.class, featurizeDataPath);
		featurizeGroundTruth.addParam("startEndPath", String.class, startEndFileNames);
		featurizeGroundTruth.addParam("frequency", Integer.class, frequency);
		// add verification
		featurizeGroundTruth.before(truncatedFileNames);
		featurizeGroundTruth.before(duplicatesFileNames);
		featurizeGroundTruth.before(eventsFileNames);
		featurizeGroundTruth.before(startEndFileNames);
		featurizeGroundTruth.after(featurizeDataPath);
		// set mode and start
		featurizeGroundTruth.prodMode();
		featurizeGroundTruth.execute();
	}
	
	private void featurizeValidateTest(Integer clusterJobNum,
			Boolean useCluster, Var featurePath,
			Array cpdAlgorithm, Array cpdFPR,
			Var featurizedFileExt, Var featurizeFunctionScriptPath,
			Var callingScriptPath, String clusterWorkspace,
			Var truncatedFileNames, Var duplicatesFileNames, Var frequency,
			Var featurizeDataPath, Var predictedCpFileNames, Array subjectIDs)
			throws Exception {
		logStep("Featurize data using predicted change points from the given algorithm");
		ExecutorBuilder featurizeChangePoints = rScript(
				featurizeFunctionScriptPath,
				callingScriptPath,
				var("featurizeUQCPD"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		featurizeChangePoints.addParam("truncatedFilePath", String.class,
				truncatedFileNames);
		featurizeChangePoints.addParam("duplicatesFilePath", String.class,
				duplicatesFileNames);
		featurizeChangePoints.addParam("frequency", Integer.class, frequency);

		Var savePath = featurePath.fileSep().cat(var(subjectIDs))
				.cat(featurizedFileExt);
		featurizeChangePoints.addParam("savePath", String.class, savePath);
		featurizeChangePoints.addParam("predictedCpPath", String.class,
				predictedCpFileNames);
		// add verification
		featurizeChangePoints.before(truncatedFileNames);
		featurizeChangePoints.before(duplicatesFileNames);
		featurizeChangePoints.before(predictedCpFileNames);
		featurizeChangePoints.after(savePath);
		// set mode and start
		featurizeChangePoints.prodMode();
		featurizeChangePoints.execute();
	}


	private void splitData(String tvtDataAssignmentPath, Array splitId,
			int numSplits, Array fileNames) throws Exception {
		logStep("Split dataset into three subsets");
		Var dataSetAssignmentPath = var(tvtDataAssignmentPath);
		// Randomly assign training and validation data
		Array subsetId = array("[0:1:3]");
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
						.cat("split").cat(String.valueOf(i)).cat(".part2.csv");
				splitDataSetInto4Parts(fileNames, part1AssignmentTablePath,
						part2AssignmentTablePath, part3AssignmentTablePath,
						part4AssignmentTablePath);
			}
		}
	}
	
	private void mergeGroundTruth(Integer clusterJobNum, 
			Boolean useCluster, Array dataSets, Array splitId, 
			String tvtDataPath, String tvtDataAssignmentPath, 
			String clusterWorkspace, Var featurePath,
			String featurizedFileExtStr) throws Exception {
		logStep("Merge featurized ground truth data to train, validate and test data set files");
		Array dataSetFileExtension = array(Arrays.asList(featurizedFileExtStr,
				featurizedFileExtStr, featurizedFileExtStr));
		Array assignment = array(Arrays.asList("0", "1", "2"));
		bind(dataSets, dataSetFileExtension, assignment);
	
		Var iterationId = var("split").cat(splitId);
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic//scratch/workspace/ObesityExperimentRScript/free.living/data/merge.data.R");
		Var trainValidateTestDataPath = var(tvtDataPath);
		Var mergeTrainDataCallingPath = trainValidateTestDataPath.fileSep()
				.cat(iterationId).fileSep().cat("merge.").cat(dataSets)
				.cat(".120.data.R");
		Var saveDataPath = trainValidateTestDataPath.fileSep().cat(iterationId)
				.fileSep().cat(dataSets).cat(".120.data.csv");
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

	private void mergeValidateTest(Integer clusterJobNum, Boolean useCluster,
			Array cpdAlgorithm, Array cpdFPR, 
			String tvtDataPath, String tvtDataAssignmentPath, 
			String featurizedFileExtStr, Array dataSets, Array splitId, 
			String clusterWorkspace, Var featurePath) throws Exception {
		logStep("Merge featurized cpd algorithm data");
		//Var featurizedFileExt = var("PureTrial.featurized.").
		//		cat(cpdAlgorithm).dot().cat(cpdFPR).dot()
		//		.cat("csv");
		Array dataSetFileExtension = array(Arrays.asList(featurizedFileExtStr,
				featurizedFileExtStr, featurizedFileExtStr));
		Array assignment = array(Arrays.asList("0", "1", "2"));
		bind(dataSets, dataSetFileExtension, assignment);
	
		Var iterationId = var("split").cat(splitId);
		Var mergeDataFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/free.living/data/merge.data.R");
		Var trainValidateTestDataPath = var(tvtDataPath);
		Var mergeTrainDataCallingPath = trainValidateTestDataPath.fileSep()
				.cat(iterationId).fileSep().cat("merge.").cat(dataSets)
				.dot().cat(cpdAlgorithm).dot().cat(cpdFPR).cat(".data.R");
		Var saveDataPath = trainValidateTestDataPath.fileSep().cat(iterationId)
				.fileSep().cat(dataSets).dot().cat(cpdAlgorithm).dot()
				.cat(cpdFPR).cat(".data.csv");
		Var dataAssignmentTablePath = var(tvtDataAssignmentPath).fileSep()
				.cat("split").cat(splitId).cat(".part").cat(assignment)
				.cat(".csv");
		ExecutorBuilder createValidateTestData = rScript(
				mergeDataFunctionScriptPath,
				mergeTrainDataCallingPath,
				var("mergeData"),
				execConfig().setParallelizable(useCluster)
						.setNumJobs(clusterJobNum)
						.setOnCluster(true)
						.setClusterWorkspace(clusterWorkspace));
		createValidateTestData.addParam("dataFileFolder", String.class, featurePath);
		createValidateTestData.addParam("dataNameFilePath", String.class,
				dataAssignmentTablePath, VerificationType.Before);
		createValidateTestData.addParam("dataFileExtension", String.class,
				var(dataSetFileExtension));
		createValidateTestData.addParam("savePath", String.class, saveDataPath,
				VerificationType.After);
		createValidateTestData.addParam("columns", List.class,
				var("c(\"WindowId\",\"File\")"));
		createValidateTestData.prodMode();
		createValidateTestData.execute();
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
	
	private void featurizeData(String expRootPath, String datasetStr,
			String frequencyStr, String rawDataPathStr,
			String tvtDataAssignmentPath, String tvtDataPath,
			String clusterWorkspace, Array cpdAlgorithms, Array cpdFPR,
			Integer clusterJobNum, Boolean useCluster, Array subjectIDs,
			String day, Array kpres) throws Exception {
		Var dataset = var(datasetStr);
		Var frequency = var(frequencyStr);
		Var rawDataPath = var(rawDataPathStr);

		logStep("Convert the timestamped data to feature vectors");
		Var expPath = var(expRootPath).fileSep().cat(dataset);
		Var featurePath = expPath.fileSep().cat("features");
		Var truncatedFileNames = expPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_truncated_day").cat(day).cat(".csv");
		Var duplicatesFileNames = expPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_").cat(frequency).cat("hz_duplicates_day").cat(day).cat(".csv");
		Var eventsFileNames = expPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_events.csv");
		Var startEndFileNames = expPath.fileSep().cat(subjectIDs).fileSep().cat(subjectIDs).cat("_start_and_end.csv");
		Var predictedCpFileNames = var(tvtDataPath).fileSep().cat("predicted_changes_").cat(cpdAlgorithms).cat("_kpre").cat(kpres).fileSep().cat(cpdFPR).fileSep().cat(subjectIDs).cat(".csv");
		
		Var featurizeFunctionScriptPath = var("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/cpd/cpd.R");
		mkdir(featurePath);
		
		int numSplits = 30;
		Array splitId = array("[0:1:" + numSplits + "]");
		Array dataSets = array(Arrays.asList("trainBase", "validateBase", "trainHMM", "testHMM"));

		Var callingScriptPath = featurePath.fileSep().cat(subjectIDs) 
				.cat(".featurize.R");
		Var featurizedFileExt = var(".featurized.csv");
		Var featurizeDataPath = featurePath.fileSep().cat(subjectIDs)
				.cat(featurizedFileExt);
		
		String featurizedFileExtStr = "PureTrial.featurized.120.csv";

		// This is where the action happens
		featurizeGroundTruth(expPath, clusterJobNum, useCluster, featurizeFunctionScriptPath, callingScriptPath, clusterWorkspace, truncatedFileNames, duplicatesFileNames, frequency, featurizeDataPath, eventsFileNames, startEndFileNames);
		featurizeValidateTest(clusterJobNum, useCluster, featurePath, cpdAlgorithms, cpdFPR, featurizedFileExt, featurizeFunctionScriptPath, callingScriptPath, clusterWorkspace, truncatedFileNames, duplicatesFileNames, frequency, featurizeDataPath, predictedCpFileNames, subjectIDs);
		//splitData(tvtDataAssignmentPath, splitId, numSplits, fileNames);
		//mergeGroundTruth(clusterJobNum, useCluster, dataSets, splitId, tvtDataPath, tvtDataAssignmentPath, clusterWorkspace, featurePath, featurizedFileExtStr);
		//mergeValidateTest(clusterJobNum, useCluster, cpdAlgorithm, cpdFPR, tvtDataPath, tvtDataAssignmentPath, featurizedFileExtStr, dataSets, splitId, clusterWorkspace, featurePath);
	}
	
	private void UQ_30Hz() throws Exception {
		String expRootPath = "/nfs/guille/wong/users/andermic/Desktop/cpd";
		String datasetStr = "uq_30Hz";
		String frequencyStr = "30";
		
		String rawDataPathStr = "/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed";
		String tvtDataPath = "/nfs/guille/wong/users/andermic/Desktop/cpd/uq_30hz";
		String tvtDataAssignmentPath = tvtDataPath + "/splits";
		
		String clusterWorkspace = "/nfs/guille/wong/users/andermic/Desktop/cpd/uq_30hz/cluster";
		Integer clusterJobNum = 100;
		Boolean useCluster = false;
		
		Array subjectIDs = array(Arrays.asList("1", "2", "3", "4", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "18", "19", "20", "21", "22", "23", "24", "25"));
		Array cpdAlgorithms = array(Arrays.asList("cc"));
		//Array cpdFPR = array(Arrays.asList("0.0001", "0.0002", "0.0003", "0.0004", "0.0005", "0.0006", "0.0007", "0.0008", "0.0009", "0.001", "0.0011", "0.0012", "0.0013", "0.0014", "0.0015", "0.0016", "0.0017", "0.0018", "0.0019", "0.002", "0.0021", "0.0022", "0.0023", "0.0024", "0.0025", "0.0026", "0.0027", "0.0028", "0.0029", "0.003", "0.0031", "0.0032", "0.0033", "0.0034", "0.0035", "0.0036", "0.0037", "0.0038", "0.0039", "0.004", "0.0041", "0.0042", "0.0043", "0.0044", "0.0045", "0.0046", "0.0047", "0.0048", "0.0049", "0.005", "0.0051", "0.0052", "0.0053", "0.0054", "0.0055", "0.0056", "0.0057", "0.0058", "0.0059", "0.006", "0.0061", "0.0062", "0.0063", "0.0064", "0.0065", "0.0066", "0.0067", "0.0068", "0.0069", "0.007", "0.0071", "0.0072", "0.0073", "0.0074", "0.0075", "0.0076", "0.0077", "0.0078", "0.0079", "0.008", "0.0081", "0.0082", "0.0083", "0.0084", "0.0085", "0.0086", "0.0087", "0.0088", "0.0089", "0.009", "0.0091", "0.0092", "0.0093", "0.0094", "0.0095", "0.0096", "0.0097", "0.0098", "0.0099", "0.01"));
		//Array cpdFPR = array(Arrays.asList("0.015", "0.02", "0.025", "0.03", "0.035", "0.04", "0.045", "0.05", "0.055", "0.06", "0.065", "0.07", "0.075", "0.08", "0.085", "0.09", "0.095"));
		Array cpdFPR = array(Arrays.asList("0.01", "0.05", "0.1"));
		Array kpres = array(Arrays.asList("30"));
		String day = "2";
		
		featurizeData(expRootPath, datasetStr, frequencyStr,
				rawDataPathStr, tvtDataAssignmentPath, tvtDataPath,
				clusterWorkspace, cpdAlgorithms, cpdFPR, clusterJobNum,
				useCluster, subjectIDs, day, kpres);
	}

	public static void main(String[] args) {
		try {
			new FeaturizeDataCPD().UQ_30Hz();
		} catch (Exception e) {
			log.error(e, e);
		}
	}
}
