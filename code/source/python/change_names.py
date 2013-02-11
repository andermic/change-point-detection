#! /usr/bin/python

from os import listdir, system

old_names = sorted([i for i in listdir('.') if i[-4:] == '.csv'])
new_names = sorted(open('names','r').read().split(', '))

if len(old_names) != len(new_names):
    print 'Old names and new names list lengths do not agree'
    print len(old_names), len(new_names)
    exit()

system('mkdir temp')
for i in range(len(old_names)):
    system('cp %s temp/scores.%s.csv' % (old_names[i], new_names[i]))
