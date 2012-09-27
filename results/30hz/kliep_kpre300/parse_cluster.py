#! /usr/bin/python

from os import listdir

# Get the names of all of the files in the current directory, excepting this code file.
file_names = listdir('.')
file_names.remove('parse_cluster.py')

for i in range(len(file_names)):
	# Give the user some progress feedback
    print i
	
	# Get the contents of a file
	lines = open(file_names[i],'r').readlines()
    
	# Find the beginning of the scores
	for j in range(len(lines)):
        if lines[j][:7] == 'Elapsed':
            score_start = j+1
	
	# Trim off some metadata at the end
    scores = lines[score_start:-3]
	
	# Get rid of some extra whitespace and the newline character on each line
    scores = [score.strip().replace(' ','') for score in scores]
	
	# Put the raw score values into a new csv file
    open('scores' + file_names[i][11:13] + '.csv', 'w').write('\n'.join(scores))
