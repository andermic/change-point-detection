#! /usr/bin/python

from os import system
from subprocess import Popen, PIPE
from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'

data_files = [i.strip() for i in open('%s/30hz_file_names.csv' % ROOT_PATH, 'r').readlines()]
event_files = [i.strip() for i in open('%s/events_file_names.csv' % ROOT_PATH, 'r').readlines()]
for i in range(len(data_files)):
    data_file_name = data_files[i].split('/')[-1]
    folder = data_files[i].split('/')[-2]
    event_file_name = '%s/%s/%s_events.csv' % (ROOT_PATH, folder, folder)

    print 'Processing subject %s' % folder
    data_head = Popen(['head','-n12', data_files[i]], stdout=PIPE).communicate()[0]
    data_tail = Popen(['tail','-n1', data_files[i]], stdout=PIPE).communicate()[0]
    events = open(event_file_name, 'r').readlines()
    raw_start_str = data_head.split('\n')[-2].split(',')[0]
    raw_end_str = data_tail.split(',')[0]
    events_start_str = events[1].split(',')[0]
    events_end_str = events[-1].split(',')[0]

    raw_start_tick = 1
    raw_end_tick = raw_start_tick + dif_ticks(raw_end_str, raw_start_str) 
    events_start_tick = raw_start_tick + dif_ticks(events_start_str, raw_start_str)
    events_end_tick = events_start_tick + dif_ticks(events_end_str, events_start_str) + int(round(float(events[-1].split(',')[2]) * 30))

    out = open('%s/%s/%s_start_and_end.csv' % (ROOT_PATH, folder, folder), 'w')
    out.write('FileType,StartDate,EndDate,StartTick,EndTick\n')
    out.write('Raw,%s,%s,%d,%d\n' % (raw_start_str, raw_end_str, raw_start_tick, raw_end_tick))
    out.write('Events,%s,%s,%d,%d\n' % (events_start_str, events_end_str, events_start_tick, events_end_tick))

    out.close()
