source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/cpd/cpd.R")
source("/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source/R/hmm/HMMlib.R")
source("/nfs/guille/wong/users/andermic/scratch/workspace/ObesityExperimentRScript/ms.osu/common.R")

# Build an HMM through supervised training
trainSupervised <- function(stateAlphabet, emissionAlphabet, stateSample, emissionSample) {
	stopifnot(length(stateSample) == length(emissionSample))
	
	transProbs = matrix(0, length(stateAlphabet), length(stateAlphabet))
	emissionProbs = matrix(0, length(stateAlphabet), length(emissionAlphabet))
	sourceCounts = rep(0,length(stateAlphabet))
	dimnames(transProbs) = list(stateAlphabet, stateAlphabet)
	dimnames(emissionProbs) = list(stateAlphabet, emissionAlphabet)
	names(sourceCounts) = stateAlphabet
	
	for (i in 1:length(stateSample)-1) {
		source = stateSample[i]
		dest = stateSample[i+1]
		transProbs[source, dest] = transProbs[source, dest] + 1
		
		e = emissionSample[i]
		emissionProbs[source,e] = emissionProbs [source,e] + 1
		
		sourceCounts[source] = sourceCounts[source] + 1
	}
	transProbs = transProbs/sourceCounts

	source = stateSample[length(stateSample)]
	e = emissionSample[length(emissionSample)]
	emissionProbs[source,e] = emissionProbs[source,e] + 1
	sourceCounts[source] = sourceCounts[source] + 1
	
	print(emissionProbs)
	print(sourceCounts)
	emissionProbs = emissionProbs/sourceCounts

	return(initHMM(States=stateAlphabet, Symbols=emissionAlphabet, transProbs=transProbs, emissionProbs=emissionProbs))
}

predictHMM <- function(formula, windowSize, trainHMMResultPath, bestModelSavePath, valiTestLabVisitFileExt, predictBaseSavePath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath) {
	CLASSES = c('lying_down', 'sitting', 'standing_household', 'walking', 'running', 'dance', 'basketball')

	# Train the HMM
	training.data <- read.csv(trainHMMResultPath)
	hmm <- trainSupervised(CLASSES, CLASSES, training.data$Real.ActivityClass, Predict)
	
	# Predict on the HMM testing data with the base classifier
	baseModel <- load(bestModelSavePath)
	summarizeModelCPD(baseModel, formula, windowSize, readData(testHMMDataInfoPath, labVisitFileFolder, valiTestLabVisitFileExt), predictBaseSavePath)

	# Predict with the HMM
	testing.data <- read.csv(predictBaseSavePath)
	testing.data$Predict <- viterbi(hmm, testing.data$Predict)
	#Need something here
	summarizeCPD(labels, predictionReportPath, confusionMatrixPath, pctConsufionMatrixPath, summaryPath)
}