#! /usr/bin/python

from make_path import make_path

#DATASET = 'OSU_HIP'
DATASET = 'UQ'
ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp/hmm'
CLASS_ALGS = ['dt','nnet','svm']
DAY = 2
ROOT_OUTPUT_FOLDER = 'results/hmm'

CLASS_ALGS = ['dt', 'svm', 'nnet']

ACC = 'MeanAccuracy'
SDA = 'SDAccuracy'
DET = 'MeanDetectionTime'
SDDET = 'SDDetectionTime'

dataset_folder = 'OSU_YR4_Hip_30Hz.HMM.7cls' if DATASET == 'OSU_HIP' else ('uq_30Hz_day%d.HMM' % DAY)
input_folder = '%s/%s' % (ROOT_INPUT_FOLDER, dataset_folder)
output_folder = '%s/%s' % (ROOT_OUTPUT_FOLDER, dataset_folder)

#WS = range(1,21)
WS = range(10,22,2)

for cls_alg in CLASS_ALGS:
    out_path = '%s/%s_results.csv' % (output_folder, cls_alg)
    make_path(out_path)
    output_fobject = open(out_path, 'w')
    output_fobject.write('WS,%s,%s,%s,%s\n' % (ACC, SDA, DET, SDDET))
    print cls_alg
    print WS
    for ws in WS:
        lines = open('%s/%s/%s.model.ws%d.best.AllWoFFT.testHMM.summary.csv' % (input_folder, cls_alg, cls_alg, ws)).readlines()
        lines = [line.strip() for line in lines]
        keys = lines[0][1:-1].split('","')
        values = [float(i) for i in lines[1].split(',')]
        file_dict = {}
        for i in range(len(keys)):
            file_dict[keys[i]] = values[i]
        output_fobject.write('%s,%f,%f,%f,%f\n' % (ws, file_dict[ACC], file_dict[SDA], file_dict[DET], file_dict[SDDET]))
    output_fobject.close()
