#! /usr/bin/python

INPUT_FOLDER = '/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls'
CLASS_ALGS = ['svm', 'dt', 'nnet']
OUTPUT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source'

ACC = 'MeanAccuracy'
DET = 'MeanTotalDetectionTime'
CPD_ALGS = ['cc', 'kliep']
FPRS = [str(i*0.0001) for i in range(1,101)]
#FPRS = [str(i*0.005) for i in range(3,20)]
#FPRS = ['0.0005','0.001','0.002','0.003','0.004']

for cls_alg in CLASS_ALGS:
    for cpd_alg in CPD_ALGS:
        output_fobject = open('%s/%s_%s_results.csv' % (OUTPUT_FOLDER, class_alg, cpd_alg), 'w')
        output_fobject.write('FPR,%s,%s\n' % (ACC, DET))
        for fpr in FPRS:
            lines = open('%s/%s/%s.model.best.AllWoFFT.%s.%s.test.summary.csv' % (INPUT_FOLDER, class_alg, class_alg, alg, fpr)).readlines()
            lines = [line.strip() for line in lines]
            keys = lines[0][1:-1].split('","')
            values = [float(i) for i in lines[1].split(',')]
            file_dict = {}
            for i in range(len(keys)):
                file_dict[keys[i]] = values[i]
            output_fobject.write('%s,%f,%f\n' % (fpr, file_dict[ACC], file_dict[DET]))
        output_fobject.close()
