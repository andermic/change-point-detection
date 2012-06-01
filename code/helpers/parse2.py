#! /usr/bin/python

from os import listdir

files = listdir('.')
files.remove('temp.py')
for i in range(len(files)):
 print '%d/%d' % (i,len(files))
 lines = open(files[i], 'r').readlines()
 lines = lines[1:]
 lines = [j.strip().split(',')[5:] for j in lines]
 labels = [lines[0][0]]
 for j in range(1,len(lines)):
  if lines[j][0] != lines[j-1][0]:
   labels.append(lines[j][0])
 label_dict = {}
 for j in range(len(labels)):
  label_dict[labels[j]] = str(j+1)
 lines = '\n'.join([label_dict[j[0]]+','+','.join(j[1:]) for j in lines])
 open(files[i][:7] + '.csv', 'w').write(lines)
 
