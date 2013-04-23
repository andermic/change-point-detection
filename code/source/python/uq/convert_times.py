#! /usr/bin/python

from os import system
from datetime import datetime, timedelta
from xlrd import xldate_as_tuple
from xldate_as_datetime import xldate_as_datetime
from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'

event_files = [i.strip() for i in open('%s/events_file_names.csv' % ROOT_PATH, 'r').readlines()]
for file_path in event_files:
    file_name = file_path.split('/')[-1]
    folder = file_path.split('/')[-2]
    system('mkdir %s/%s' % (ROOT_PATH, folder))
    print folder

    data = open(file_path, 'r').readlines()[1:]
    out = open('%s/%s/%s_events2.csv' % (ROOT_PATH, folder, folder), 'w')
    out.write('Time,StartTick,Interval,ActivityCode,CumulativeStepCount,ActivityScore\n')

    milli = 0
    split_line = data[0].split(',')
    d = xldate_as_tuple(float(split_line[0]), 0)
    raw_start = list(d[:3]) + [3,0,0]
    dt = datetime(*d)
    dt_str = dt.strftime('%d/%m/%Y %H:%M:%S.000') 
    offset = dif_ticks(dt_str, datetime(*raw_start).strftime('%d/%m/%Y %H:%M:%S.000'))

    split_line[0] = dt_str
    split_line[1] = str(1 + offset)
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
        split_line[1] = str(int(round(((int(split_line[1]) + 1) / 2.0 * 3.0))) + offset)
        out.write(','.join(split_line))
    
    out.close()
