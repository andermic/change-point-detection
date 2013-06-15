#! /usr/bin/python

from os import listdir
from make_path import make_path

DATASET = 'OSU_HIP'
#DATASET = 'UQ'
ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp/cpd'
CLASS_ALGS = ['nnet','svm','dt']
DAY = 2
ROOT_OUTPUT_FOLDER = 'results/cpd'
CPD_ALGS = ['cc','kliep']

ACC = 'MeanAccuracy'
SDA = 'SDAccuracy'
DET = 'MeanDetectionTime'
SDDET = 'SDDetectionTime'

dataset_folder = ('OSU_YR4_Hip_30Hz.ws120.7cls' if DATASET == 'OSU_HIP' else ('uq_30Hz_day%d' % DAY))
input_folder = '%s/%s' % (ROOT_INPUT_FOLDER, dataset_folder)
output_folder = '%s/%s' % (ROOT_OUTPUT_FOLDER, dataset_folder)

FPRS = [str(i*0.0001) for i in range(1,101)]
FPRS = ['0.0005','0.001', '0.0017', '0.0019', '0.0021', '0.0024', '0.0028', '0.0033', '0.005','0.01']

for cls_alg in CLASS_ALGS:
    for cpd_alg in CPD_ALGS:
        files = listdir('%s/%s' % (input_folder, cls_alg))
        files = [file for file in files if cpd_alg in file and 'test.summary' in file and ('0.' + file.split('.')[-4]) in FPRS]
        fprs = ['0.' + file.split('.')[-4] for file in files]
        print cls_alg, cpd_alg
        print fprs
        out_path = '%s/%s_%s_results.csv' % (output_folder, cls_alg, cpd_alg)
        make_path(out_path)
        output_fobject = open(out_path, 'w')
        output_fobject.write('FPR,%s,%s,%s,%s\n' % (ACC, SDA, DET, SDDET))
        for fpr in fprs:
            lines = open('%s/%s/%s.model.best.AllWoFFT.%s.%s.test.summary.csv' % (input_folder, cls_alg, cls_alg, cpd_alg, fpr)).readlines()
            lines = [line.strip() for line in lines]
            keys = lines[0][1:-1].split('","')
            values = [float(i) for i in lines[1].split(',')]
            file_dict = {}
            for i in range(len(keys)):
                file_dict[keys[i]] = values[i]
            output_fobject.write('%s,%f,%f,%f,%f\n' % (fpr, file_dict[ACC], file_dict[SDA], file_dict[DET], file_dict[SDDET]))
        output_fobject.close()
