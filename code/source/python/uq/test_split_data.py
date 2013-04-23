#! /usr/bin/python

# Concatenate split events files together, to compare them against original
#  events files

from os import listdir

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'
folders = range(1,26)
folders.remove(5)
folders.remove(17)

for f in folders:
    files = [file for file in listdir('%s/%d' % (ROOT_PATH, f)) if 'events_' in file]
    files.sort(key=lambda x: int(x.split('day')[-1].split('.')[0]))
    out = open('%s/test/%d_test.csv' % (ROOT_PATH, f), 'w')
    for file in files:
        contents = open('%s/%d/%s' % (ROOT_PATH, f, file), 'r').read()
        out.write(contents)
