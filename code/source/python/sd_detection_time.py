#! /usr/bin/python

from math import sqrt
from os import listdir, getcwd
from make_path import make_path

IS_HMM = True
ALGS = ['dt','svm','nnet']
ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp'
ROOT_OUTPUT_FOLDER = '%s/%s' % (getcwd(), 'results')

add_hmm = '.HMM' if IS_HMM else ''
add_cpd = '.ws120' if not IS_HMM else ''
exp_folder = 'hmm' if IS_HMM else 'cpd'
dataset_folder = 'OSU_YR4_Hip_30Hz' + add_hmm + add_cpd + '.7cls' 
input_folder = '%s/%s/%s' % (ROOT_INPUT_FOLDER, exp_folder, dataset_folder)

def sd_dev(list_):
    mean = float(sum(list_)) / len(list_)
    return sqrt(sum([(val - mean) ** 2 for val in list_]) / (len(list_) - 1))

for alg in ALGS:
    files = listdir('%s/%s' % (input_folder, alg))
    files = [file for file in files if ('test' + add_hmm.replace('.','') + '.accuracy.csv') in file]
    for file in files:
        lines = open('%s/%s/%s' % (input_folder, alg, file), 'r').readlines()[1:]
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
        out_path = '%s/%s/%s/%s/%s' % (ROOT_OUTPUT_FOLDER, exp_folder, dataset_folder, alg, out)
        make_path(out_path)
        stream = open(out_path, 'w')
        stream.write('"MeanAccuracy","SDAccuracy","MeanDetectionTime","SDDetectionTime"\n')
        stream.write('%f,%f,%f,%f\n' % (mean_accuracy, sd_accuracy, mean_det_time, sd_det_time))
        stream.close()
