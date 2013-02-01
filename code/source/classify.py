#! /usr/bin/python

SWEM_PREDICTIONS_ROOT = '/nfs/guille/wong/wonglab2/obesity/2012/msexp/OSU_YR4_Hip_30Hz.ws10.7cls/svm.linear.stacking.mv'
SWEM_PREDICTIONS_FILENAME = 'svm.AllWoFFT.best.stacking.model.test.csv'

CP_PREDICTIONS_ROOT = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/results/30Hz'
CP_PREDICTIONS_CC_PATH = 'predicted_changes_cc'

#SVM_PREDICTIONS_ROOT = 
#SVM_PREDICTIONS_FILENAME = 

print len(open(SWEM_PREDICTIONS_ROOT + '/' + 'split0' + '/' + SWEM_PREDICTIONS_FILENAME, 'r').readlines())
