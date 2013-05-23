#! /usr/bin/python

from math import sqrt
from os import listdir, mkdir, getcwd

IS_HMM = True
DATASET = 'OSU_HIP'
#DATASET = 'UQ'
DAY = 2
ALGS = ['dt','svm','nnet']
ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp'
ROOT_OUTPUT_FOLDER = getcwd()

add_hmm = '.HMM' if IS_HMM else ''
exp_folder = 'hmm' if IS_HMM else 'cpd'
data_set_folder = ('OSU_YR4_Hip_30Hz' + add_hmm + '.7cls') if dataset == 'OSU_HIP' else (('uq_30Hz_day%d' % DAY) + add_hmm)
input_folder = '%s/%s/%s' % (ROOT_FOLDER, exp_folder, dataset_folder)

def sd_dev(list_):
    mean = float(sum(list_)) / len(list_)
    return sqrt(sum([(val - mean) ** 2 for val in list_]) / (len(list_) - 1))

for alg in ALGS:
    files = listdir('%s/%s' % (INPUT_FOLDER, alg))
    files = [file for file in files if ('test' + add_hmm.replace('.','') + '.accuracy.csv') in file]
    for file in files:
        lines = open('%s/%s/%s' % (INPUT_FOLDER, alg, file), 'r').readlines()[1:]
        lines = [i.strip().split(',') for i in lines]
        accuracies = [float(i[0]) for i in lines]
        tdts = [float(i[1]) for i in lines]
        dss = [float(i[2]) for i in lines]
        mean_accuracy = sum(accuracies) / len(accuracies)
        sd_accuracy = sd_dev(accuracies)
        det_times = [tdts[i] / dss[i] * 120 for i in range(len(tdts))]
        mean_det_time = sum(det_times) / len(det_times)
        sd_det_time = sd_dev(det_times)
        out = file.split('.')
        out[-2] = 'summary'
        out = '.'.join(out)
        if not alg in listdir(OUTPUT_FOLDER):
            mkdir('%s/%s' % (OUTPUT_FOLDER, alg))
        stream = open('%s/%s/%s' % (OUTPUT_FOLDER, alg, out), 'w')
        stream.write('"MeanAccuracy","SDAccuracy","MeanDetectionTime","SDDetectionTime"\n')
        stream.write('%f,%f,%f,%f\n' % (mean_accuracy, sd_accuracy, mean_det_time, sd_det_time))
        stream.close()
