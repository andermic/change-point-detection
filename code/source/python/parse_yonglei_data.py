#! /usr/bin/python

# Some preprocessing for the OSU_HIP dataset

from os import listdir

# Get the names of all of the files in the current directory, excepting this code file.
files = listdir('.')
files.remove('parse_cluster_results.py')

for i in range(len(files)):
 # Give the user some progress feedback
 print '%d/%d' % (i,len(files))

 # Get the contents of a file
 lines = open(files[i], 'r').readlines()
 
 # Trim first line of metadata
 lines = lines[1:]
 
 # Trim newline character, split the comma separated values that we need into an array
 lines = [j.strip().split(',')[5:] for j in lines]
 
 # Get the first class label
 labels = [lines[0][0]]
 
 # Iterate through each line of data. When a new class label is found, add it to a list of labels.
 for j in range(1,len(lines)):
  if lines[j][0] != lines[j-1][0]:
   labels.append(lines[j][0])
 
 # Don't care what the value of each label actually was, just give each label its own numeric value
 label_dict = {}
 for j in range(len(labels)):
  label_dict[labels[j]] = str(j+1)
 
 # Put each line of the read file back into csv format, with only the necessary columns and labels mapped
 # to their corresponding numeric values.
 lines = '\n'.join([label_dict[j[0]]+','+','.join(j[1:]) for j in lines])
 
 # Put the new csv string into a new file
 open(files[i][:7] + '.csv', 'w').write(lines)
