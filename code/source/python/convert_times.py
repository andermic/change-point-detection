#! /usr/bin/python

from os import system
from datetime import datetime, timedelta
from xlrd import xldate_as_tuple
from xldate_as_datetime import xldate_as_datetime

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'

event_files = [i.strip() for i in open('%s/events_file_names.csv' % ROOT_PATH, 'r').readlines()]
for file_path in event_files:
    file_name = file_path.split('/')[-1]
    folder = file_path.split('/')[-2]
    system('mkdir %s' % folder)

    data = open(file_path, 'r').readlines()[1:]
    out = open('%s/%s/%s_events.csv' % (ROOT_PATH, folder, folder), 'w')
    out.write('Time,DataCount,Interval,ActivityCode,CumulativeStepCount,ActivityScore\n')

    milli = 0
    split_line = data[0].split(',')
    d = xldate_as_tuple(float(split_line[0]), 0)
    dt = datetime(*d)
    split_line[0] = dt.strftime('%d/%m/%Y %H:%M:%S.000')
    out.write(','.join(split_line))
    
    for i in range(1, len(data)):
        split_line = data[i].split(',')
        interval = float(data[i-1].split(',')[2])
        left_radix = int(interval)
        dt += timedelta(seconds=left_radix) 
        milli += int(round((interval - left_radix) * 1000))
        if milli >= 1000:
            dt += timedelta(seconds=1)
            milli -= 1000
        split_line[0] = dt.strftime('%d/%m/%Y %H:%M:%S.') + ('%3d' % milli).replace(' ', '0')
        split_line[1] = str(int(round((int(split_line[1]) / 2.0 * 3.0))))
        out.write(','.join(split_line))
    
    out.close()
