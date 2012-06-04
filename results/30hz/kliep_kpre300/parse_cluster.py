#! /usr/bin/python

from os import listdir

file_names = listdir('.')
file_names.remove('parse_cluster.py')
for i in range(len(file_names)):
    print i
    lines = open(file_names[i],'r').readlines()
    for j in range(len(lines)):
        if lines[j][:7] == 'Elapsed':
            score_start = j+1
    scores = lines[score_start:-3]
    scores = [score.strip().replace(' ','') for score in scores]
    open('scores' + file_names[i][11:13] + '.csv', 'w').write('\n'.join(scores))
