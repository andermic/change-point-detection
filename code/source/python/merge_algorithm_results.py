#! /usr/bin/python

INPUT_FOLDER = '/nfs/guille/wong/wonglab3/obesity/2012/cpd/OSU_YR4_Hip_30Hz.ws120.7cls'
MODEL_FOLDER = 'svm.linear'
MODEL_PATH = INPUT_FOLDER + '/' + MODEL_FOLDER
OUTPUT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/code/source'

ACC = 'MeanAccuracy'
DET = 'MeanTotalDetectionTime'
ALGORITHMS = ['cc', 'kliep']
FPRS = ['0.0005','0.001','0.002','0.003'.'0.004']

for alg in ALGORITHMS:
    output_fobject = open('%s/%s_results.csv' % (OUTPUT_FOLDER, alg), 'w')
    output_fobject.write('FPR,%s,%s' % (ACC, DET))
    for fpr in FPRS:
        lines = open('svm.model.best.AllWoFFT.%s.%s.test.summary.csv' % (ALGORITHMS, FPRS)).readlines()
        lines = [line.strip() for line in lines]
        keys = lines[0][1:-1].split('","')
        values = [float(i) for i in lines[1].split(',')]
        file_dict = {}
        for i in range(len(keys)):
            file_dict[keys[i]] = file_dict[values[i]]
        output_fobject.write('%s,%f,%f' % (fpr, file_dict[ACC], file_dict[DET]))
    output_fobject.close()
