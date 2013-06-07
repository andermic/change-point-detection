source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/cpd/cpd.R")
source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/hmm/HMMlib.R")
source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/common.R")

# Build an HMM through supervised training
trainSupervised <- function(stateAlphabet, emissionAlphabet, stateSample, emissionSample) {
	stopifnot(length(stateSample) == length(emissionSample))
	
	transProbs <- matrix(1, length(stateAlphabet), length(stateAlphabet))
	emissionProbs <- matrix(1, length(stateAlphabet), length(emissionAlphabet))
	sourceCounts <- rep(0,length(stateAlphabet))
	dimnames(transProbs) <- list(stateAlphabet, stateAlphabet)
	dimnames(emissionProbs) <- list(stateAlphabet, emissionAlphabet)
	names(sourceCounts) <- stateAlphabet
	
	for (i in 1:(length(stateSample)-1)) {
		source = as.character(stateSample[i])
		dest = as.character(stateSample[i+1])
		transProbs[source, dest] <- transProbs[source, dest] + 1

		e <- as.character(emissionSample[i])
		emissionProbs[source,e] <- emissionProbs[source,e] + 1
		
		sourceCounts[source] <- sourceCounts[source] + 1
	}
	print("transProbs")
	print(transProbs)
	print("")
	print("emissionProbs")
	print(emissionProbs)
	print("")
	print("sourceCounts")
	print(sourceCounts)
	print("")
	transProbs = transProbs/(sourceCounts + length(stateAlphabet))

	source <- as.character(stateSample[length(stateSample)])
	e <- as.character(emissionSample[length(emissionSample)])
	emissionProbs[source,e] <- emissionProbs[source,e] + 1
	sourceCounts[source] <- sourceCounts[source] + 1
	
	emissionProbs <- emissionProbs/(sourceCounts + length(emissionAlphabet))

	return(initHMM(States=as.factor(stateAlphabet), Symbols=as.factor(emissionAlphabet), transProbs=transProbs, emissionProbs=emissionProbs))
}

predictHMM <- function(
		labels,
		formula,
		windowSize,
		trainHMMResultPath,
		bestModelSavePath,
		testHMMDataInfoPath,
		labVisitFileFolder,
		labVisitFileExt,
		predictBasePath,
		predictHMMPath,
		confusionMatrixPath,
		pctConsufionMatrixPath,
		summaryPath) {

	print('training hmm')	
	training.data <- read.csv(trainHMMResultPath)
	hmm <- trainSupervised(labels, labels, training.data$Real.ActivityClass, training.data$Predict)
	print(hmm)
	
	print('predicting on the hmm testing data with the base classifier')
	load(bestModelSavePath)
	summarizeModelCPD(model, formula, windowSize, readData(testHMMDataInfoPath, labVisitFileFolder, labVisitFileExt), predictBasePath)

	print('predicting with the hmm')
	testing.data <- read.csv(predictBasePath)
	testing.data$Predict <- viterbi(hmm, as.character(testing.data$Predict))
	write.csv(testing.data, predictHMMPath)
	
	print('summarizing results')
	summarizeCPD(labels, predictHMMPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath)
}