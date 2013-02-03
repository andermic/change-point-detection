#! /usr/bin/python

from os import listdir

#SWEM or SVM
CLASSIFIER_ALG = 'SWEM'

#Control Chart or KLIEP
CPD_ALG = 'Control Chart'

CLASSIFIER_ROOT = '/nfs/guille/wong/wonglab2/obesity/2012/msexp/OSU_YR4_Hip_30Hz.ws10.7cls/svm.linear.stacking.mv'
CLASSIFIER_FILENAMES = {'SWEM':'svm.AllWoFFT.best.stacking.model.test.csv'}
SUBJECTID = 0
TRIALID = 1
WINDOWID = 2
REAL = 3
PREDICT = 4
NUM_SPLITS = 30

CPD_ALG_ROOT = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/results/30hz'
CPD_ALG_PATHS = {'Control Chart':'predicted_changes_cc'}

#SVM_PREDICTIONS_ROOT = 
#SVM_PREDICTIONS_FILENAME = 

# Get predicted change points
cpd_path = CPD_ALG_ROOT + '/' + CPD_ALG_PATHS[CPD_ALG]
fprs = listdir(cpd_path)
pred_cps = {}
if CPD_ALG == 'Control Chart':
    for folder in fprs:
        pred_cps[folder] = {}
        for file in listdir(cpd_path + '/' + folder):
            cur_path = cpd_path + '/' + folder + '/' + file
            cur_preds = open(cur_path, 'r').read().strip().split(',')
            pred_cps[folder][file] = list(set([int(round(int(i)/300.)) for i in cur_preds]))

print pred_cps.keys()
for key in pred_cps.keys():
    print len(pred_cps[key].keys())
key1 = pred_cps.keys()[1]
key2 = pred_cps[key1].keys()[40]
print key1, key2
print pred_cps[key1][key2]
print
print


# Get classifier algorithm data
class_data = []
for cur_split in range(NUM_SPLITS):
    class_data.append({})
    cur_path = CLASSIFIER_ROOT + '/' + ('split%d' % cur_split) + '/' + CLASSIFIER_FILENAMES['SWEM']
    cur_data = [i.strip().split(',') for i in open(cur_path, 'r').readlines()[1:]]
    for line in cur_data:
        series = line[SUBJECTID] + '_V%d' % ((int(line[TRIALID])+5) / 6)
        if class_data[cur_split].get(series) == None:
            class_data[cur_split][series] = {}
        class_data[cur_split][series][line[WINDOWID]] = {'real':line[REAL], 'predict':line[PREDICT]}

print len(class_data)
for i in class_data:
    print i.keys(),
    print
    print i[i.keys()[0]]
    break

# Calculate accuracy
for fpr in fprs:
    fpr_acc = 0
    for cur_split in range(NUM_SPLITS):
        split_acc = 0
        for series in class_data[cur_split].keys():
            
        #split_acc /= #insert shit here
        fpr_acc += split_acc
    fpr_acc /= 30
