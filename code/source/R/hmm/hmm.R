source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/cpd/cpd.R")
source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/hmm/HMMlib.R")
source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/common.R")

# Build an HMM through supervised training
trainSupervised <- function(stateAlphabet, emissionAlphabet, stateSample, emissionSample) {
	stopifnot(length(stateSample) == length(emissionSample))
	
	transProbs = matrix(1, length(stateAlphabet), length(stateAlphabet))
	emissionProbs = matrix(1, length(stateAlphabet), length(emissionAlphabet))
	sourceCounts = rep(0,length(stateAlphabet))
	dimnames(transProbs) = list(stateAlphabet, stateAlphabet)
	dimnames(emissionProbs) = list(stateAlphabet, emissionAlphabet)
	names(sourceCounts) = stateAlphabet
	
	for (i in 1:length(stateSample)-1) {
		source = stateSample[i]
		dest = stateSample[i+1]
		transProbs[source, dest] = transProbs[source, dest] + 1
		
		e = emissionSample[i]
		emissionProbs[source,e] = emissionProbs[source,e] + 1
		
		sourceCounts[source] = sourceCounts[source] + 1
	}
	transProbs = transProbs/(sourceCounts + length(stateAlphabet))

	source = stateSample[length(stateSample)]
	e = emissionSample[length(emissionSample)]
	emissionProbs[source,e] = emissionProbs[source,e] + 1
	sourceCounts[source] = sourceCounts[source] + 1
	
	emissionProbs = emissionProbs/(sourceCounts + length(emissionAlphabet))

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

	# Train the HMM
	training.data <- read.csv(trainHMMResultPath)
	training.data <-
	hmm <- trainSupervised(labels, labels, training.data$Real.ActivityClass, training.data$Predict)
	
	# Predict on the HMM testing data with the base classifier
	load(bestModelSavePath)
	summarizeModelCPD(model, formula, windowSize, readData(testHMMDataInfoPath, labVisitFileFolder, labVisitFileExt), predictBasePath)

	# Predict with the HMM
	testing.data <- read.csv(predictBasePath)
	testing.data$Predict <- viterbi(hmm, as.character(testing.data$Predict))
	write.csv(testing.data, predictHMMPath)
	
	# Summarize results
	summarizeCPD(labels, predictHMMPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath)
}