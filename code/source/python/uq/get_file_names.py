#! /usr/bin/python

import os
import re

INPUT_FOLDER = '/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/full'
ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'
OUTPUT_EVENTS = '%s/events_file_names.csv' % ROOT_PATH
OUTPUT_1 = '%s/1hz_file_names.csv' % ROOT_PATH
OUTPUT_30 = '%s/30hz_file_names.csv' % ROOT_PATH

contents = os.listdir(INPUT_FOLDER)
folders = []
for content in contents:
    try:
        folders.append(str(int(content)))
    except:
        pass

folders.sort(key=lambda x: int(x))

events = open(OUTPUT_EVENTS, 'w')
one_hz = open(OUTPUT_1, 'w')
thirty_hz = open(OUTPUT_30, 'w')
for folder in folders:
    path = INPUT_FOLDER + '/' + folder
    for file in os.listdir(INPUT_FOLDER + '/' + folder):
        if re.search('Events.csv', file) != None:
            events.write(path + '/' + file + '\n')
        if re.search('secDataTable.csv', file) != None:
            one_hz.write(path + '/' + file + '\n')
        if re.search('RAW.csv', file) != None:
            thirty_hz.write(path + '/' + file + '\n')
        
events.close()
one_hz.close()
thirty_hz.close()
