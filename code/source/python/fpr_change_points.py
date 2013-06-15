#! /usr/bin/python

# Generate predicted change-points given some cpd scores and a list of false positive rates.

from os import listdir, system
from datetime import datetime


def binary_search(list_, val, start, end):
    try:
        if val >= list_[(start+end)/2] and val < list_[(start+end)/2+1]:
            return (start+end)/2
        elif val < list_[(start+end)/2]:
            return binary_search(list_, val, start, (start+end)/2)
        else:
            return binary_search(list_, val, (start+end)/2+1, end)
    except IndexError:
        print list_[0]
        print list_[-1]
        print 'val: %d' % val
        print 'start: %d' % start
        print 'end: %d' % end
        exit()

FPR_LIST = [0.0005, 0.001, 0.005, 0.01]
FPR_LIST = [0.0024, 0.0028, 0.0033]
DATASET = 'uq'
KPRE = 300
GRANULARITY = 3
ALGORITHM = 'cc'
#ROOT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/results/30hz'
ROOT_FOLDER = '/nfs/guille/wong/users/andermic/uq/changepoints'
INPUT_PATH = ROOT_FOLDER + '/' + '%s_kpre%d' % (ALGORITHM, KPRE)
OUTPUT_FOLDER = 'predicted_changes_%s_kpre%d' % (ALGORITHM, KPRE)
OUTPUT_PATH = ROOT_FOLDER + '/' + OUTPUT_FOLDER
if OUTPUT_FOLDER in listdir(ROOT_FOLDER):
    print 'There are existing results at %s.\n' % OUTPUT_PATH
    print 'These results will not be overwritten by this program, so delete them manually to continue.'
    #exit()

PROCESSED_FOLDER = '/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed'
folders = []
for i in listdir(PROCESSED_FOLDER):
    try:
        folders.append(str(int(i)))
    except:
        pass
folders.sort(key=lambda x: int(x))

score_files = ['%s/scores_%s.csv' % (INPUT_PATH, folder) for folder in folders]
events_files = ['%s/%s/%s_events.csv' % (PROCESSED_FOLDER, folder, folder) for folder in folders]
se_files = ['%s/%s/%s_start_and_end.csv' % (PROCESSED_FOLDER, folder, folder) for folder in folders]

system('mkdir ' + OUTPUT_PATH)

for fpr in FPR_LIST:
    fpr_path = OUTPUT_PATH + '/' + str(fpr) 
    if str(fpr) not in listdir(OUTPUT_PATH):
        system('mkdir ' + fpr_path)

    for file_num in range(len(score_files)):
        print '\nprocessing file %d\n' % file_num
        print 'reading data'
        cur_scores_pre = [float(i) for i in open(score_files[file_num], 'r').read().split('\n') if i != '']
        print 'done reading data\n'
        fp_num = int(round(fpr*len(cur_scores_pre)))
        score_len = len(cur_scores_pre)
        print 'enumerating data'
        cur_scores_pre = enumerate(cur_scores_pre)
        print 'done enumerating data\n'

        print 'removing excess scores'
        cur_scores = []
        while True: 
            try:
                for j in range(GRANULARITY-1):
                    cur_scores_pre.next()
                cur_scores.append(cur_scores_pre.next())
            except StopIteration:
                break
        #cur_scores = (cur_scores[i] for i in range(score_len) if i % GRANULARITY == 0)
        print 'done removing excess scores\n'

        print 'sorting scores'
        cur_scores.sort(key=lambda x:x[1], reverse=True)
        print 'done sorting\n'
        cur_fps = 0
        cps = []
        if DATASET == 'osu_hip':
            have_changed = [True] + [False]*5 #For ticks 0,3600,7200,10880,14400,18000
            for score in cur_scores:
                cur_score = score[0] + KPRE + 1  #Add 1 to compensate for MATLAB/R 1-based indexing
                changed_index = (cur_score - 1) / 3600
                if have_changed[changed_index]:
                    cur_fps += 1
                    if cur_fps > fp_num:
                        break
                else:
                    have_changed[changed_index] = True
                cps.append(str(cur_score))
        elif DATASET == 'uq':
            se_lines = open(se_files[file_num], 'r').readlines()
            se_raw = se_lines[1].strip().split(',')[1:]
            se_events = se_lines[2].strip().split(',')[1:]
            raw_start_tick = int(se_raw[2])
            raw_end_tick = int(se_raw[3])
            events_start_tick = int(se_events[2])
            events_end_tick = int(se_events[3])

            events = open(events_files[file_num], 'r').readlines()
            have_changed = [True] + [False] * len(events)
            event_ticks = [1]
            event_ticks += [int(events[i].split(',')[1]) for i in range(1, len(events))]
            data_end = event_ticks[-1] + int(round(float(events[-1].split(',')[2]) * 30))
            event_ticks.append(data_end)
            print 'raw_end_tick: %d' % raw_end_tick
            #event_ticks = [tick for tick in event_ticks if tick > KPRE and tick < raw_end_tick]
            for score in cur_scores:
                index = score[0] + KPRE + 1  #Add 1 to compensate for MATLAB/R 1-based indexing
                if index >= data_end:
                    continue
                try:
                    changed_index = binary_search(event_ticks, index, 0, len(event_ticks)-2)
                except RuntimeError:
                    print 'Recursion depth exceeded'
                    print 'event_ticks[0]: %d' % event_ticks[0]
                    print 'event_ticks[-1]: %d' % event_ticks[-1]
                    print index
                    exit()
                if have_changed[changed_index]:
                    cur_fps += 1
                    if cur_fps > fp_num:
                        break
                else:
                    have_changed[changed_index] = True
                cps.append(str(index))
        
        cps.sort(key=lambda x: int(x)) 
        out = open('%s/%s' % (fpr_path, score_files[file_num].split('_')[-1]), 'w')
        out.write('ChangePointPredictions\n' + '\n'.join(cps))
        out.close()
