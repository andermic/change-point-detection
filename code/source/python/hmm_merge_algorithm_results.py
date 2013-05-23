#! /usr/bin/python

INPUT_FOLDER = '/nfs/guille/wong/users/andermic/my_exp/hmm/OSU_YR4_Hip_30Hz.HMM.7cls'
CLASS_ALGS = ['dt', 'svm', 'nnet']
OUTPUT_FOLDER = '.'

ACC = 'MeanAccuracy'
SDA = 'SDAccuracy'
DET = 'MeanDetectionTime'
SDDET = 'SDDetectionTime'

WS = range(1,21)

for cls_alg in CLASS_ALGS:
    output_fobject = open('%s/hmm_%s_results.csv' % (OUTPUT_FOLDER, cls_alg), 'w')
    output_fobject.write('"WS",%s,%s\n' % (ACC, DET))
    for ws in WS:
        lines = open('%s/%s/%s.model.ws%d.best.AllWoFFT.testHMM.summary.csv' % (INPUT_FOLDER, cls_alg, cls_alg, ws)).readlines()
        lines = [line.strip() for line in lines]
        keys = lines[0][1:-1].split('","')
        values = [float(i) for i in lines[1].split(',')]
        file_dict = {}
        for i in range(len(keys)):
            file_dict[keys[i]] = values[i]
        output_fobject.write('%s,%f,%f,%f,%f\n' % (ws, file_dict[ACC], file_dict[SDA], file_dict[DET], file_dict[SDDET]))
    output_fobject.close()
