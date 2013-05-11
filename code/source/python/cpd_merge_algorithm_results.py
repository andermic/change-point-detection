#! /usr/bin/python

from os import listdir

#INPUT_FOLDER = '/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls'
INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp/cpd/OSU_YR4_Hip_30Hz.ws120.7cls'
CLASS_ALGS = ['dt','nnet','svm']
OUTPUT_FOLDER = '.'

ACC = 'MeanAccuracy'
SDA = 'SDAccuracy'
DET = 'MeanDetectionTime'
SDDET = 'SDDetectionTime'
CPD_ALGS = ['cc','kliep']

FPRS = [str(i*0.0001) for i in range(1,101)]
FPRS = [0.0005,0.001,0.005,0.01]

for cls_alg in CLASS_ALGS:
    for cpd_alg in CPD_ALGS:
        files = listdir('%s/%s' % (INPUT_FOLDER, cls_alg))
        files = [file for file in files if cpd_alg in file and 'test.summary' in file]
        fprs = ['0.' + file.split('.')[-4] for file in files]
        print cls_alg, cpd_alg
        print fprs
        output_fobject = open('%s/%s_%s_results.csv' % (OUTPUT_FOLDER, cls_alg, cpd_alg), 'w')
        output_fobject.write('FPR,%s,%s,%s,%s\n' % (ACC, SDA, DET, SDDET))
        for fpr in fprs:
            lines = open('%s/%s/%s.model.best.AllWoFFT.%s.%s.test.summary.csv' % (INPUT_FOLDER, cls_alg, cls_alg, cpd_alg, fpr)).readlines()
            lines = [line.strip() for line in lines]
            keys = lines[0][1:-1].split('","')
            values = [float(i) for i in lines[1].split(',')]
            file_dict = {}
            for i in range(len(keys)):
                file_dict[keys[i]] = values[i]
            output_fobject.write('%s,%f,%f,%f,%f\n' % (fpr, file_dict[ACC], file_dict[SDA], file_dict[DET], file_dict[SDDET]))
        output_fobject.close()
