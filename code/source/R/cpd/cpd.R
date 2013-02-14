source("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/common.R")
source("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/free.living/data/featurize.data.R")
source("/nfs/guille/u2/a/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/svm.exp.R")
library('e1071', lib.loc='/nfs/guille/wong/wonglab3/obesity/2012/cpd')
#library(nnet)
library(rpart)
#library(glmnet)

# Modified from featurizeMstmData in mstm/mstm.featurize.data.R
featurizeCPD <- function(rawDataFilePath, frequency, changePointsPath, savePath) {
	MINIMUM_SIZE <- 1
	rawData <- read.csv(rawDataFilePath)

	# Featurize using either ground truth or predicted change points
	if (changePointsPath == '') {
		endRows <- as.matrix(seq(120 * frequency, nrow(rawData), 120 * frequency))
	}
	else {
		endRows <- as.matrix(read.csv(changePointsPath)) - 1
	}
	if (endRows[nrow(endRows), ] != nrow(rawData)) {
		endRows <- rbind(endRows, nrow(rawData))
	}

	# Exclude change points that are too near to a previous change point prediction
	exclude <- NULL
	for (i in 2:nrow(endRows)) {
		if ((endRows[i] - endRows[i-1]) < MINIMUM_SIZE) {
			exclude <- c(exclude,i)
		} 
	}
	if (!is.null(exclude)) {
		endRows = as.matrix(endRows[-exclude])
	}
	
	#print(endRows)
	#print(rawData)
	startRow <- 1
	df <- data.frame()
	windowId <- 1
	for (endRow in endRows) {
		#print(paste("start", as.character(startRow)))
		#print(paste("end", as.character(endRow)))
		#print(paste("window: startRow", as.character(startRow), "endRow", as.character(endRow)))
		#print(paste("scale: start", as.character(start), "end", as.character(end)))
		#print(paste("end", as.character(endRow)))
		window <- rawData[startRow:endRow, ]
		#print(nrow(window))
		#print(window)
		df <- rbind(df, cbind(data.frame(WindowId=windowId, Scale=(endRow - startRow + 1), SubseqId=1), featurizeWindowCPD(frequency, window)))
		startRow <- endRow + 1
		windowId <- windowId + 1
	}
	#print(df)
	write.csv(df, savePath, row.names=FALSE, quote=FALSE)
}


# Modified from featurizeWindow in mstm/mstm.featurize.data.R
featurizeWindowCPD <- function(frequency, window) {
	trialId <- paste(as.character(unique(window$TrialID)), collapse=" ")

	ac = window$ActivityClass
	acu = unique(ac)
	activityRatio <- NULL
	for (activity in acu) {
		activityRatio <- c(activityRatio, length(which(ac==activity)) / length(ac)) 
	}
	activityRatio <- paste(as.character(activityRatio), collapse=" ")
	activityClass <- paste(as.character(acu), collapse=" ")
	#print(activityClass)
	
	#print("Axis1")
	axis1 <- addNoiseToStraightLine(window$Axis1)
	a1 <- featurizeAxis(frequency, axis1)
	a1 <- addAxisNameToDataFrame(a1, "Axis1")
	
	#print("Axis2")
	axis2 <- addNoiseToStraightLine(window$Axis2)
	a2 <- featurizeAxis(frequency, axis2)
	a2 <- addAxisNameToDataFrame(a2, "Axis2")
	
	#print("Axis3")
	axis3 <- addNoiseToStraightLine(window$Axis3)
	a3 <- featurizeAxis(frequency, axis3)
	a3 <- addAxisNameToDataFrame(a3, "Axis3")
	
	c12 <- correlationBetweenAccelerometerAxes(axis1, axis2)
	c13 <- correlationBetweenAccelerometerAxes(axis1, axis3)
	c23 <- correlationBetweenAccelerometerAxes(axis2, axis3)
	
	#print(window$DateTime[1])
	#print(window$DateTime[nrow(window)])
	window$DateTime <- as.character(window$DateTime)
	metaData <- data.frame(SubjectID=window$SubjectID[1], LabVisit=window$LabVisit[1], File=window$File[1], StartTime=window$DateTime[1], EndTime=window$DateTime[nrow(window)], TrialID=trialId, ActivityClass=activityClass, ActivityRatio=activityRatio)
	row <- cbind(metaData, a1, a2, a3, data.frame(CorrelationAxis1Axis2=c12, CorrelationAxis1Axis3=c13, CorrelationAxis2Axis3=c23))
	stopifnot(length(which(!complete.cases(row))) <= 6) # Not using the two FFT features per axis anyway. Works only for AllWoFFT formula.
	return(row)
}

# Modified from....
classificationAccuracyCPD <- function(real, pred) {
	dataSize = sum(real$Scale)
	accuracy <- 0
	for (i in 1:length(pred)) {
		realClasses = unlist(strsplit(as.character(real$ActivityClass[i]), " "))
		realRatios = unlist(strsplit(as.character(real$ActivityRatios[i]), " "))
		if (pred[i] %in% realClasses) {
			accuracy <- accuracy + as.numeric(realRatios[which(realClasses == pred[i])]) * real$Scale[i] / dataSize
		}
	}
	return(accuracy)
}

# Calculate detection times for the given set of predicted and real values
detectionTime <- function(real, pred) {
	lastActivity <- ''
	curTimeTick <- 1
	realActivities <- NULL
	realTimeTicks <- NULL
	for (i in 1:nrow(real)) {
		curActivities <- unlist(strsplit(as.character(real$ActivityClass[i]), " "))
		ratios <- unlist(strsplit(as.character(real$ActivityRatios[i]), " "))
		for (j in 1:length(curActivities)) {
			if (curActivities[j] != lastActivity) {
				realActivities <- c(realActivities, curActivities[j])
				realTimeTicks <- c(realTimeTicks, curTimeTick)  
				lastActivity <- curActivities[j]				
			} 
			curTimeTick <- curTimeTick + as.numeric(ratios[j]) * real$Scale[i]
		}
	}

	predTimeTicks <- 1
	for (i in 1:nrow(real)) {
		predTimeTicks <- c(predTimeTicks, predTimeTicks[length(predTimeTicks)] + real$Scale[i])
	}
	realTimeTicks <- c(realTimeTicks, predTimeTicks[length(predTimeTicks)] + 1)

	totalDetectionTime <- 0	
	for (i in 1:(length(realTimeTicks) - 1)) {
		#print(sprintf('%d - %s', realTimeTicks[i], realActivities[i]))
		didDetect <- FALSE
		for (j in 1:(length(predTimeTicks) - 1)) {
			if ((predTimeTicks[j] >= realTimeTicks[i]) && (predTimeTicks[j] < realTimeTicks[i+1]) && (pred[j] == realActivities[i])) {
				#print('  Found change')
				#print(sprintf('  %d - +%d', predTimeTicks[j], predTimeTicks[j] - realTimeTicks[i]))
				didDetect <- TRUE
				totalDetectionTime <- totalDetectionTime + predTimeTicks[j] - realTimeTicks[i]
				break
			}
		}
		if (!didDetect) {
			#print('  Did not find change')
			#print(sprintf('  +%d', realTimeTicks[i+1] - realTimeTicks[i]))
			totalDetectionTime <- totalDetectionTime + realTimeTicks[i+1] - realTimeTicks[i]
		}
	}

	return(data.frame(TotalDetectionTime=totalDetectionTime, DataSize=predTimeTicks[length(predTimeTicks)]))
}


# Modified from quickTrainValidate in ms.osu/svm.exp.R
quickTrainValidateCPD <- function(
		formula,  
		scale=NA, 
		trainDataInfoPath, 
		validateDataInfoPath, 
		labVisitFileFolder,
		trainingLabVisitFileExt,
		valiTestLabVisitFileExt,
		kernal="radial",
		gamma, 
		cost,
		validateSummaryPath) {
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt, scale)
	print("Training data read")
	
	#print(training.data)
	#print(nrow(training.data))
	if (kernal=="radial") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, kernel=kernal, gamma=gamma, cost=cost)
	} else if (kernal=="linear") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, kernel=kernal, cost=cost)
	} else {
		stop(paste("Invalid kernal:", kernal))
	}
	
	print("Model trained")
	#print(modelSavePath)
	#save(model, file=modelSavePath)
	#print("Model saved")

	validate.data <- readData(validateDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt, scale)
	#print(nrow(validate.data))
	pred <- predict(model, featureMatrix(validate.data, formula))
	real <- data.frame(ActivityClass=validate.data$ActivityClass, ActivityRatios=validate.data$ActivityRatio, Scale=validate.data$Scale)
	#print(length(real))
	accuracy <- classificationAccuracyCPD(real, pred)
	write.csv(data.frame(Accuracy=accuracy), validateSummaryPath, row.names = FALSE)
}


# Modified from testBestSingleScale in ms.osu/svm.exp.R
testBestModelCPD <- function(
		validateSummaryFile, 
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiTestLabVisitFileExt,
		kernal,
		bestModelInfo,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath) {
	validateSummary <- read.csv(validateSummaryFile)
	validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=120))
	bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
	write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	print("Training data read")
	if (kernal=="radial") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, 
				kernel=kernal, gamma=bestModelInfo$Gamma, cost=bestModelInfo$Cost)
	} else if (kernal=="linear") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, 
				kernel=kernal, cost=bestModelInfo$Cost)
	} else {
		stop(paste("Invalid kernal:", kernal))
	}
	
	print("Model trained")
	save(model, file=bestModelSavePath)
	
	print("Test model on training data")
	summarizeModelCPD(model, formula, 120, training.data, trainReportPath)
	
	print("Test model on validation data")
	summarizeModelCPD(model, formula, 120, 
			readData(validateDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			validateReportPath, bestModelInfo$ValidateAccuracy)
	
	print("Test model on testing data")
	summarizeModelCPD(model, formula, 120, 
			readData(testDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			testReportPath)
}

# Modified from testBestSingleScale in ms.osu/svm.exp.R
testBestModelCPD <- function(
		validateSummaryFile, 
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiTestLabVisitFileExt,
		kernal,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath) {
	validateSummary <- read.csv(validateSummaryFile)
	validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=120))
	bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
	print(bestModelInfo)
	write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	print("Training data read")
	if (kernal=="radial") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, 
				kernel=kernal, gamma=bestModelInfo$Gamma, cost=bestModelInfo$Cost)
	} else if (kernal=="linear") {
		model <- svm(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, 
				kernel=kernal, cost=bestModelInfo$Cost)
	} else {
		stop(paste("Invalid kernal:", kernal))
	}
	
	print("Model trained")
	save(model, file=bestModelSavePath)
	
	print("Test model on training data")
	summarizeModelCPD(model, formula, 120, training.data, trainReportPath)
	
	print("Test model on validation data")
	summarizeModelCPD(model, formula, 120, 
			readData(validateDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			validateReportPath, bestModelInfo$ValidateAccuracy)
	
	print("Test model on testing data")
	summarizeModelCPD(model, formula, 120, 
			readData(testDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			testReportPath)
}

# Modified from testBestSingleScale in ms.osu/svm.exp.R
testBestModelNnetCPD <- function(
		validateSummaryFile, 
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiTestLabVisitFileExt,
		kernal,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath) {
	#validateSummary <- read.csv(validateSummaryFile)
	#validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=120))
	#bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
	#print(bestModelInfo)
	#write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	#training.data$ActivityClass <- factor(training.data$ActivityClass)
	print("Training data read")
	#my_data <- data.frame(cbind(featureMatrix(training.data, formula), Class=training.data$ActivityClass))
	my_data <- data.frame(as.data.frame(featureMatrix(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
	
	model <- nnet(formula=ActivityClass~., data=my_data, size=15)
	#model <- glmnet(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, family="multinomial", alpha=1)
	
	print("Model trained")
	save(model, file=bestModelSavePath)
		
	print("Test model on training data")
	summarizeModelCPD(model, formula, 120, training.data, trainReportPath)
	
	print("Test model on validation data")
	summarizeModelCPD(model, formula, 120, 
			readData(validateDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			validateReportPath)
	
	print("Test model on testing data")
	summarizeModelCPD(model, formula, 120, 
			readData(testDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			testReportPath)
}

# Modified from testBestSingleScale in ms.osu/svm.exp.R
testBestModelDtCPD <- function(
		validateSummaryFile, 
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiTestLabVisitFileExt,
		kernal,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath,
		classAlg) {
	#validateSummary <- read.csv(validateSummaryFile)
	#validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=120))
	#bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
	#print(bestModelInfo)
	#write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	#training.data$ActivityClass <- factor(training.data$ActivityClass)
	print("Training data read")
	#my_data <- data.frame(cbind(featureMatrix(training.data, formula), Class=training.data$ActivityClass))
	my_data <- data.frame(as.data.frame(featureMatrix(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
	
	model <- rpart(formula=ActivityClass~., data=my_data)
	#model <- glmnet(x=featureMatrix(training.data, formula), y=training.data$ActivityClass, family="multinomial", alpha=1)
	
	print("Model trained")
	save(model, file=bestModelSavePath)
		
	print("Test model on training data")
	summarizeModelCPD(model, formula, 120, training.data, trainReportPath)
	
	print("Test model on validation data")
	summarizeModelCPD(model, formula, 120, 
			readData(validateDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			validateReportPath)
	
	print("Test model on testing data")
	summarizeModelCPD(model, formula, 120, 
			readData(testDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), 
			testReportPath)
}

# Modified from summarizeSingleScaleModel in ms.osu/svm.exp.R
summarizeModelCPD <- function(model, formula, scale, testData, predictionReportPath, expectedAccuracy=NA) {
	real <- data.frame(ActivityClass=testData$ActivityClass, ActivityRatios=testData$ActivityRatio, Scale=testData$Scale)
	pred <- as.character(predict(model, data.frame(featureMatrix(testData, formula)), type='class'))
	
	#WORST HACK EVAR!!!
	#data <- data.frame(featureMatrix(testData, formula))
	#my_data <- featureMatrix(testData, formula)
	#print('my_data start:')
	#print(my_data)
	#print('my_data end:')
	#write.csv(my_data, paste(predictionReportPath,'.tmp',sep=""))
	#my_data <- read.csv(paste(predictionReportPath,'.tmp',sep=""))
	#stopifnot(FALSE)
	#pred <- as.character(predict(model, my_data, type='class'))

	#print(pred)
	#stopifnot(FALSE)
	if (!is.na(expectedAccuracy)) {
		accuracy <- classificationAccuracyCPD(real, pred)
		#print(accuracy)
		#print(expectedAccuracy)
		stopifnot(abs(expectedAccuracy-accuracy) < 0.01)
	}
	#print(colnames(testData)[1:100])
	prediction <- testData[,c("SubjectID", "TrialID", "WindowId")]
	if ("SubseqId" %in% colnames(testData)) {
		prediction <- cbind(prediction, data.frame(SubseqId=testData$SubseqId))
	}
	prediction <- cbind(prediction, data.frame(Scale=scale, Real=real,Predict=pred))
	colnames(prediction)[ncol(prediction)] <- "Predict"
	write.csv(prediction, predictionReportPath, row.names = FALSE)
}


# Modified from summarize in ms.osu/glmnet.single.scale.R 
summarizeCPD <- function(labels, predictionReportPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath) {
	prediction <- read.csv(predictionReportPath)
	real <- data.frame(ActivityClass=prediction$Real.ActivityClass, ActivityRatios=prediction$Real.ActivityRatios, Scale=prediction$Real.Scale)
	pred <- prediction$Predict
	cm <- confusionMatrix(real, pred, labels)
	write.csv(cm, confusionMatrixPath, row.names = TRUE)
	confusionMatrixNumToPct(confusionMatrixPath, pctConsufionMatrixPath)
	accuracy <- classificationAccuracyCPD(real, pred)
	dt <-detectionTime(real, pred)
	#write.csv(data.frame(Accuracy=accuracy), summaryPath, row.names = FALSE)
	write.csv(data.frame(Accuracy=accuracy, TotalDetectionTime=dt$TotalDetectionTime, DataSize=dt$DataSize), summaryPath, row.names = FALSE)
}

# Modified from mergeSplitShuffle in function/merge.split.shuffle.R
mergeSplitShuffleCPD <- function(
		bestModelResFilePath, 
		windowSize=NA, trialGroup=NA, scale=NA, formula=NA, alpha=NA, additionalFilter=NA, 
		testDataSet, expectedNumEntries, 
		accuracySavePath, 
		meanAccuracySavePath, 
		confusionMatrixSavePath, 
		pctConfusionMatrixSavePath,
		reportSavePath=NA) {
	
	allRes <- read.csv(bestModelResFilePath)
	filter <- data.frame(Formula=formula, TestDataSet=testDataSet, Alpha=alpha)
	if (!is.na(windowSize)) {
		filter <- cbind(filter, data.frame(WindowSize=windowSize))
	}
	if (!is.na(trialGroup)) {
		filter <- cbind(filter, data.frame(TrialGroup=trialGroup))
	}
	if (!is.na(scale)) {
		filter <- cbind(filter, data.frame(Scale=scale))
	}
	filter <- cbind(filter, additionalFilter)
	#print(filter)
	res <- df.match(allRes, filter)
	#res <- allRes[which(allRes$WindowSize==windowSize & allRes$TrialGroup==trialGroup & allRes$Formula==formula & allRes$TestDataSet==testDataSet),]
	#if (!is.na(alpha)) {
	#	res <- res[which(res$Alpha==alpha),]
	#}
	#if (!is.na(scale)) {
	#	res <- res[which(res$Scale==scale),]
	#}
	#print(nrow(res))
	stopifnot(nrow(res) == expectedNumEntries)
	allAcc <- data.frame(Accuracy=NULL,TotalDetectionTime=NULL,DataSize=NULL)
	allCm <- NA
	testReport <- data.frame()
	for (i in 1 : nrow(res)) {
		#print(i)
		accuracyFile <- as.character(res$TestAccuracyFile[i])
		#if (file.exists(accuracyFile)) {
		print(accuracyFile)
		accuracyData <- read.csv(accuracyFile)
		#accuracy <- accuracyData$Accuracy[1]
		allAcc <- rbind(allAcc, accuracyData)
		
		confusionMatrixFile  <- as.character(res$TestConfusionMatrixFile[i])
		#print(confusionMatrixFile)
		confusionMatrixData <- read.csv(confusionMatrixFile)
		labels <- as.character(confusionMatrixData[,1])
		confusionMatrixData <- data.frame(confusionMatrixData[,2:length(confusionMatrixData)], row.names=labels)
		if (is.na(allCm)) {
			allCm <- confusionMatrixData
		} else {
			allCm <- allCm + confusionMatrixData
		}
		
		if (!is.na(reportSavePath)) {
			testReport <- rbind(testReport, read.csv(as.character(res$TestReportFile)))
		}
		#}
	}
	
	write.csv(allAcc, accuracySavePath, row.names = FALSE)
	
	averageAccuray <- data.frame(MeanAccuracy=mean(allAcc$Accuracy), SDAccuracy=sd(allAcc$Accuracy), MeanTotalDetectionTime=(mean(allAcc$TotalDetectionTime/allAcc$DataSize)*120))
	write.csv(averageAccuray, meanAccuracySavePath, row.names = FALSE)
	
	write.csv(allCm, confusionMatrixSavePath, row.names = TRUE)
	confusionMatrixNumToPct(confusionMatrixSavePath, pctConfusionMatrixSavePath)
	
	if (!is.na(reportSavePath)) {
		write.csv(testReport, reportSavePath, row.names = FALSE)
	}
}

#TODO: Complete this sometime?
mergeAlgorithmResults <- function(meanAccuracyPath, fprsString, summarySavePath) {
	fprs <- as.numeric(unlist(strsplit(fprsString, " ")))
}

# Modified from ms.osu/common.R
featureMatrixNoFFT <- function(data, formula) {
	# Horrible hack to keep containSpecialCase check, while getting rid of FFT features
	data$DominantFrequency_Axis1 <- NULL
	data$DominantFrequency_Axis2 <- NULL
	data$DominantFrequency_Axis3 <- NULL
	data$DominantFrequencyAmplitude_Axis1 <- NULL
	data$DominantFrequencyAmplitude_Axis2 <- NULL
	data$DominantFrequencyAmplitude_Axis3 <- NULL
	
	stopifnot(!containSpecialCase(data))
	return(data.matrix(data[attr(terms(as.formula(formula)), "term.labels")]))
}