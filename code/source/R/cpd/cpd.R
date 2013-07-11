source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/common.R")
source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/free.living/data/featurize.data.R")
source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/svm.exp.R")
library('e1071', lib.loc='/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/lib/')
library(nnet)
library(rpart)

# Modified from featurizeMstmData in mstm/mstm.featurize.data.R
featurizeCPD <- function(rawDataFilePath, frequency, savePath, changePointsPath=NA, windowSize=NA, hmm='false') {
	rawData <- read.csv(rawDataFilePath)

	# Featurize using either fixed or variable window sizes
	if (is.na(changePointsPath)) {
		stopifnot(!is.na(windowSize))
		endRows <- as.matrix(seq(windowSize * frequency, nrow(rawData), windowSize * frequency))
	}
	else {
		endRows <- as.matrix(read.csv(changePointsPath)) - 1
	}
	
	dataEnd = nrow(rawData)

	# If the last row of the data isn't the end of a window, make it so
	if (endRows[nrow(endRows), ] != dataEnd) {
		endRows <- rbind(endRows, dataEnd)
	}

	df <- data.frame()
	windowId <- 1
	for (endRow in endRows) {
		window <- rawData[startRow:endRow, ]
		df <- rbind(df, cbind(data.frame(WindowId=windowId, Scale=(endRow - startRow + 1), SubseqId=1), featurizeWindowCPD(frequency, window)))
		startRow <- endRow + 1
		windowId <- windowId + 1
	}
	
	if(hmm == 'true') {
		exclude <- NULL
		for (i in 1:nrow(df)) {
			if(length(unlist(strsplit(as.character(df$TrialID[i]),' '))) > 1) {
				exclude <- c(exclude,i)
			} 
		}
		if (!is.null(exclude)) {
			df = df[-exclude,]
		}
	}
	
	write.csv(df, savePath, row.names=FALSE, quote=FALSE)
}


# Modified from featurizeWindow in mstm/mstm.featurize.data.R
featurizeWindowCPD <- function(frequency, window, hmm='false') {
	trialId <- paste(as.character(unique(window$TrialID)), collapse=" ")

	ac = window$ActivityClass
	acu = unique(ac)
	
	activityRatio <- NULL
	for (activity in acu) {
		activityRatio <- c(activityRatio, length(which(ac==activity)) / length(ac)) 
	}
	activityRatio <- paste(as.character(activityRatio), collapse=" ")
	activityClass <- paste(as.character(acu), collapse=" ")
	
	if(hmm == 'true') {
		if(length(acu) > 1) {
			activityClass <- NA
		}
	}

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
	FREQ <- 30

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

	print('predTimeTicks[length(predTimeTicks)]')
	print(predTimeTicks[length(predTimeTicks)])
	print('totalDetectionTime')
	print(totalDetectionTime)
	print('length(realActivities)')
	print(length(realActivities))
	return(totalDetectionTime / length(realActivities) / 30)
}


# Modified from quickTrainValidate in ms.osu/svm.exp.R
quickTrainValidateCPD <- function(
		algorithm,
		formula,  
		scale=NA, 
		trainDataInfoPath, 
		validateDataInfoPath, 
		labVisitFileFolder,
		trainingLabVisitFileExt,
		valiLabVisitFileExt=trainingLabVisitFileExt,
		kernal="linear",
		validateSummaryPath,
		Gamma=NA,
		Cost=NA,
		NumHiddenUnits=NA,
		WeightDecay=NA) {
	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt, scale)
	print("Training data read")

	if (algorithm == "svm") {	
		if (kernal=="radial") {
			stopifnot(!(is.na(Gamma) || is.na(Cost)))
			model <- svm(x=featureMatrixNoFFT(training.data, formula), y=training.data$ActivityClass, kernel=kernal, gamma=Gamma, cost=Cost)
		} else if (kernal=="linear") {
			stopifnot(!is.na(Cost))
			model <- svm(x=featureMatrixNoFFT(training.data, formula), y=training.data$ActivityClass, kernel=kernal, cost=Cost)
		} else {
			stop(paste("Invalid kernal:", kernal))
		}
	}
	else if (algorithm == "nnet") {
		stopifnot(!(is.na(NumHiddenUnits) || is.na(WeightDecay)))
		my_data <- data.frame(as.data.frame(featureMatrixNoFFT(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
		model <- nnet(formula=ActivityClass~., data=my_data, size=NumHiddenUnits, decay = WeightDecay, maxit = 100000, MaxNWts=1000000)
	}
	else {
		stop('Bad algorithm')
	}
	print("Model trained")

	validate.data <- readData(validateDataInfoPath, labVisitFileFolder, valiLabVisitFileExt, scale)
    write.csv(featureMatrixNoFFT(validate.data, formula), 'test.csv')
	pred <- predict(model, featureMatrixNoFFT(validate.data, formula), type='class')
	real <- data.frame(ActivityClass=validate.data$ActivityClass, ActivityRatios=validate.data$ActivityRatio, Scale=validate.data$Scale)
	accuracy <- classificationAccuracyCPD(real, pred)
	write.csv(data.frame(Accuracy=accuracy), validateSummaryPath, row.names = FALSE)
}


# Modified from testBestSingleScale in ms.osu/svm.exp.R
testBestModelCPD <- function(
		algorithm,
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiLabVisitFileExt=trainingLabVisitFileExt,
		testLabVisitFileExt=trainingLabVisitFileExt,
		kernal,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath,
		windowSize=NA,
		validateSummaryFile=NA,
		bestModelInfo=NA) {

	if (!is.na(validateSummaryFile)) {
		validateSummary <- read.csv(validateSummaryFile)
		validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=windowSize))
		bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
		write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	}

	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	print("Training data read")

	if (algorithm == "svm") {
		if (kernal=="radial") {
			model <- svm(x=featureMatrixNoFFT(training.data, formula), y=training.data$ActivityClass, 
					kernel=kernal, gamma=bestModelInfo$Gamma, cost=bestModelInfo$Cost)
		} else if (kernal=="linear") {
			model <- svm(x=featureMatrixNoFFT(training.data, formula), y=training.data$ActivityClass, 
					kernel=kernal, cost=bestModelInfo$Cost)
		} else {
			stop(paste("Invalid kernal:", kernal))
		}
	}
	else if (algorithm == "nnet") {
		my_data <- data.frame(as.data.frame(featureMatrixNoFFT(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
		model <- nnet(formula=ActivityClass~., data=my_data, maxit=100000, MaxNWts=1000000, size=bestModelInfo$NumHiddenUnits, decay=bestModelInfo$WeightDecay)
	}
	else if (algorithm == "dt") {
		my_data <- data.frame(as.data.frame(featureMatrixNoFFT(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
		model <- rpart(formula=ActivityClass~., data=my_data)
	}
	#else if (algorithm == "logr") {
	#	model <- glmnet(x=featureMatrixNoFFT(training.data, formula), y=training.data$ActivityClass, family="multinomial", alpha=1)
	#}
	else {
		stop('Bad algorithm')
	}
		
	print("Model trained")
	save(model, file=bestModelSavePath)
	
	print("Test model on training data")
	summarizeModelCPD(model, formula, windowSize, training.data, trainReportPath)
	
	print("Test model on validation data")
	if (algorithm == 'nnet') {
		bestModelInfo <- NA  # Since nnet package uses random initial weights when building models, accuracy is variable and should not be verified
	}
	summarizeModelCPD(model, formula, windowSize, 
			readData(validateDataInfoPath, labVisitFileFolder, valiLabVisitFileExt), 
			validateReportPath, bestModelInfo)
	
	print("Test model on testing data")
	summarizeModelCPD(model, formula, windowSize, 
			readData(testDataInfoPath, labVisitFileFolder, testLabVisitFileExt), 
			testReportPath)
}


# Modified from summarizeSingleScaleModel in ms.osu/svm.exp.R
summarizeModelCPD <- function(model, formula, scale, testData, predictionReportPath, bestModelInfo=NA) {
	real <- data.frame(ActivityClass=testData$ActivityClass, ActivityRatios=testData$ActivityRatio, Scale=testData$Scale)
	pred <- as.character(predict(model, data.frame(featureMatrixNoFFT(testData, formula)), type='class'))

	if (!is.na(bestModelInfo[1])) {
		accuracy <- classificationAccuracyCPD(real, pred)
		stopifnot(abs(bestModelInfo$ValidateAccuracy-accuracy) < 0.01)
	}
	#print(colnames(testData)[1:100])
	prediction <- testData[,c("SubjectID", "TrialID", "WindowId")]
	if ("SubseqId" %in% colnames(testData)) {
		prediction <- cbind(prediction, data.frame(SubseqId=testData$SubseqId))
	}
	prediction <- cbind(prediction, data.frame(Scale=scale, Real=real, Predict=pred))
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
	dt <- detectionTime(real, pred)
	#write.csv(data.frame(Accuracy=accuracy), summaryPath, row.names = FALSE)
	write.csv(data.frame(Accuracy=accuracy, DetectionTime=dt), summaryPath, row.names = FALSE)
}


# Modified from mergeSplitShuffle in function/merge.split.shuffle.R
mergeSplitShuffleCPD <- function(
		bestModelResFilePath, 
		windowSize=NA, trialGroup=NA, scale=NA,
		formula=NA, alpha=NA, additionalFilter=NA, 
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
	res <- df.match(allRes, filter)
	#res <- allRes[which(allRes$WindowSize==windowSize & allRes$TrialGroup==trialGroup & allRes$Formula==formula & allRes$TestDataSet==testDataSet),]
	#if (!is.na(alpha)) {
	#	res <- res[which(res$Alpha==alpha),]
	#}
	#if (!is.na(scale)) {
	#	res <- res[which(res$Scale==scale),]
	#}
	stopifnot(nrow(res) == expectedNumEntries)
	allAcc <- data.frame(Accuracy=NULL,DetectionTime=NULL)
	allCm <- NA
	testReport <- data.frame()
	for (i in 1 : nrow(res)) {
		#print(i)
		accuracyFile <- as.character(res$TestAccuracyFile[i])
		#if (file.exists(accuracyFile)) {
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
	
	detectionTimes = allAcc$DetectionTime
	averageAccuracy <- data.frame(MeanAccuracy=mean(allAcc$Accuracy), SDAccuracy=sd(allAcc$Accuracy), MeanDetectionTime=mean(detectionTimes), SDDetectionTime=sd(detectionTimes))
	write.csv(averageAccuracy, meanAccuracySavePath, row.names = FALSE)
	
	write.csv(allCm, confusionMatrixSavePath, row.names = TRUE)
	confusionMatrixNumToPct(confusionMatrixSavePath, pctConfusionMatrixSavePath)
	
	if (!is.na(reportSavePath)) {
		write.csv(testReport, reportSavePath, row.names = FALSE)
	}
}


# Modified from ms.osu/common.R
featureMatrixNoFFT <- function(data, formula) {
	# Keeps containSpecialCase check, while getting rid of FFT features
	data$DominantFrequency_Axis1 <- NULL
	data$DominantFrequency_Axis2 <- NULL
	data$DominantFrequency_Axis3 <- NULL
	data$DominantFrequencyAmplitude_Axis1 <- NULL
	data$DominantFrequencyAmplitude_Axis2 <- NULL
	data$DominantFrequencyAmplitude_Axis3 <- NULL
	
	stopifnot(!containSpecialCase(data))
	return(data.matrix(data[attr(terms(as.formula(formula)), "term.labels")]))
}


# Modified from featurizeMstmData in mstm/mstm.featurize.data.R
featurizeUQCPD <- function(day, truncatedDataFilePath, duplicatesDataFilePath, frequency, savePath, eventsPath, predictedCpPath=NA, windowSize = NA, hmm='false') {
	print('reading data')
	trunc <- read.csv(truncatedDataFilePath)
	dup <- read.csv(duplicatesDataFilePath)
	events <- read.csv(eventsPath)
	GRANULARITY <- 3
	
	day <- as.numeric(day)
	if (day == 1) {
		offset <- 0
		day_len = 21 * 3600 * 30
	}
	else {
		offset = ((day - 1) * 24 - 3) * 3600 * 30
		day_len <- 24 * 3600 * 30 
	}
	

	# Featurize using either fixed or variable window sizes
	if (is.na(windowSize)) {
		if(is.na(predictedCpPath)) {
			endRows <- events$StartTick - 1
		}
		else {
			endRows <- read.csv(predictedCpPath)$ChangePointPredictions - 1
		}
		endRows = endRows - offset
		endRows = endRows[which(endRows>0 & endRows<=day_len)]

		# Make the last row of the data an end row, if it isn't already
		if (day_len - endRows[length(endRows)] >= GRANULARITY) {
			endRows <- c(endRows, day_len)
		}
	}
	else {
		endRows <- seq(windowSize * frequency, day_len, windowSize * frequency)
	}

	data <- data.frame(SubjectID=rep(day, day_len), LabVisit=day,
	 File=truncatedDataFilePath, DateTime=(offset+1):(offset+day_len),
	 TrialID=NA, ActivityClass=NA, Axis1=NA, Axis2=NA, Axis3=NA,
	 stringsAsFactors=FALSE)
	
	data[trunc$Tick-offset, c('Axis1','Axis2','Axis3')] <- trunc[,2:4]
	data[which(is.na(data$Axis1)), 'Axis1'] <- rep(dup[, 'Axis1'], dup$Interval) 
	data[which(is.na(data$Axis2)), 'Axis2'] <- rep(dup[, 'Axis2'], dup$Interval)
	data[which(is.na(data$Axis3)), 'Axis3'] <- rep(dup[, 'Axis3'], dup$Interval)
	data$ActivityClass = rep(events$ActivityCode, events$Interval)
	
	print('featurizing data')
	startRow <- 1
	df <- data.frame()
	windowId <- 1
	for (endRow in endRows) {
		print(endRow)
		window <- data[startRow:endRow, ]
		df <- rbind(df, cbind(data.frame(WindowId=windowId, Scale=(endRow - startRow + 1), SubseqId=1), featurizeWindowCPD(frequency, window, hmm)))
		startRow <- endRow + 1
		windowId <- windowId + 1
	}

	if(hmm == 'true') {
		df = df[-which(is.na(df$ActivityClass)), ]
	}
	
	write.csv(df, savePath, row.names=FALSE, quote=FALSE)
}

mergeDataUQ <- function(dataFileFolder, dataNameFilePath, dataFileExtension, savePath, columns=NA) {
	dataNames <- read.csv(dataNameFilePath)
	df <- data.frame()
	for (i in 1 : nrow(dataNames)) {
		file <- file.path(dataFileFolder, paste(dataNames[i,1], dataFileExtension, sep=""))
		print(file)
		data <- read.csv(file)
		if (!is.na(columns)) {
			data <- unique(data[,columns])
			names(data)[2] = 'File'
			data['File'] = dataNames[i,]
		}
		df <- rbind(df, data)
	}
	write.csv(df, savePath, row.names=FALSE, quote=FALSE)
	
	mergeInfo <- data.frame(DataFolder=dataFileFolder, FileList=dataNameFilePath, FileExt=dataFileExtension)
	write.csv(mergeInfo, paste(savePath, ".info.csv", sep=""), row.names=FALSE, quote=FALSE)
}

featurizeTiming <- function(numRuns, ticksPerWindow, min, max) {
	for (i in 1:numRuns) {
		data = data.frame(Axis1=matrix(runif(ticksPerWindow, min, max)), Axis2=matrix(runif(ticksPerWindow, min, max)), Axis3=matrix(runif(ticksPerWindow, min, max)), trialId=1, ActivityClass=1, ActivityRatio=1, LabVisit=1, SubjectID=1, File=1, DateTime=1)
		featurizeWindowCPD(30, data);
	}
}

# For timing experiments
predictTiming <- function(model, formula, scale, testData, predictionReportPath, bestModelInfo=NA) {
	#real <- data.frame(ActivityClass=testData$ActivityClass, ActivityRatios=testData$ActivityRatio, Scale=testData$Scale)
	print('predicting')
	print(system.time(pred <- as.character(predict(model, data.frame(featureMatrixNoFFT(testData, formula)), type='class'))));
	print('predicted')

	#if (!is.na(bestModelInfo[1])) {
	#	accuracy <- classificationAccuracyCPD(real, pred)
	#	stopifnot(abs(bestModelInfo$ValidateAccuracy-accuracy) < 0.01)
	#}
	#print(colnames(testData)[1:100])
	#prediction <- testData[,c("SubjectID", "TrialID", "WindowId")]
	#if ("SubseqId" %in% colnames(testData)) {
	#	prediction <- cbind(prediction, data.frame(SubseqId=testData$SubseqId))
	#}
	#prediction <- cbind(prediction, data.frame(Scale=scale, Real=real, Predict=pred))
	#colnames(prediction)[ncol(prediction)] <- "Predict"
	#write.csv(prediction, predictionReportPath, row.names = FALSE)
}

# For timing experiments
testBestModelTiming <- function(
		algorithm,
		formula, formulaName, labels, 
		split,
		trainDataInfoPath, 
		validateDataInfoPath, 
		testDataInfoPath, 
		labVisitFileFolder,
        trainingLabVisitFileExt,
		valiLabVisitFileExt=trainingLabVisitFileExt,
		testLabVisitFileExt=trainingLabVisitFileExt,
		kernal,
		bestModelInfoSavePath,
		bestModelSavePath,
		trainReportPath, 
		validateReportPath, 
		testReportPath,
		windowSize=NA,
		validateSummaryFile=NA,
		bestModelInfo=NA) {

	if (!is.na(validateSummaryFile)) {
		validateSummary <- read.csv(validateSummaryFile)
		validateSummary <- df.match(validateSummary, data.frame(Split=split, Formula=formulaName, Scale=windowSize))
		bestModelInfo <- validateSummary[which.max(validateSummary$ValidateAccuracy),]
		#write.csv(bestModelInfo, bestModelInfoSavePath, row.names = FALSE)
	}

	training.data <- readData(trainDataInfoPath, labVisitFileFolder, trainingLabVisitFileExt)
	print("Training data read")

	print("Model trained")
	if (algorithm == "dt") {
		my_data <- data.frame(as.data.frame(featureMatrixNoFFT(training.data, formula)),data.frame(ActivityClass=training.data$ActivityClass))
		model <- rpart(formula=ActivityClass~., data=my_data)
	}
	else {
		load(bestModelSavePath)
	}
	
	print("Test model on testing data")
	predictTiming(model, formula, windowSize, 
			readData(testDataInfoPath, labVisitFileFolder, testLabVisitFileExt), 
			testReportPath)
}
