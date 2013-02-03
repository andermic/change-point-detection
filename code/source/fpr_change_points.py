#! /usr/bin/python

# Generate some change-points for a list of given false positive rates, given
# some cpd scores.

from os import listdir, system

FPR_LIST = [0.0001,0.0005,0.001]
KPRE = 300
INPUT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/results/30hz/kliep_kpre%d' % KPRE
ALGORITHM = 'kliep'
OUTPUT_FOLDER = 'predicted_changes_' + ALGORITHM
OUTPUT_PATH = INPUT_FOLDER + '/' + OUTPUT_FOLDER
if OUTPUT_FOLDER in listdir(INPUT_FOLDER):
    print 'There are existing results at %s.\n' % OUTPUT_PATH
    print 'These results will not be overwritten by this program, so delete them manually to continue.'
    exit()
system('mkdir ' + OUTPUT_PATH)

score_files = [i for i in listdir(INPUT_FOLDER) if i[:6] == 'scores']
# Assume here that all score files contain the same number of entries
score_size = len(open(INPUT_FOLDER + '/' + score_files[1], 'r').readlines())

count = 0

for fpr in FPR_LIST:
    fpr_path = OUTPUT_PATH + '/' + str(fpr) 
    if str(fpr) not in listdir(OUTPUT_PATH):
        system('mkdir ' + fpr_path)

    fp_num = int(round(fpr*score_size))
    for score_file in score_files:
        cur_scores = [float(i) for i in open(INPUT_FOLDER + '/' + score_file, 'r').read().split('\n')]
        cur_scores = list(enumerate(cur_scores))
        cur_scores.sort(key=lambda x:x[1], reverse=True)
        cur_scores = [(i[0] + KPRE + 1, i[1]) for i in cur_scores] #Add 1 to compensate for MATLAB's 1-based indexing
        cur_fps = 0
        have_changed = [True] + [False]*5 #For ticks 0,3600,7200,10880,14400,18000
        cps = []
        for score in cur_scores:
            changed_index = (score[0] - 1) / 3600
            if have_changed[changed_index]:
                cur_fps += 1
                if cur_fps > fp_num:
                    break
            else:
                have_changed[changed_index] = True
            cps.append(str(score[0]))

        fstream = open(fpr_path + '/' + score_file[7:], 'w')
        fstream.write('PredictedChanges\n' + '\n'.join(cps))
        fstream.close()
