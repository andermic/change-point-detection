#! /usr/bin/python

# Split already truncated data into single day sections

from datetime import datetime
from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed'
FREQ = 30
TICKS_PER_DAY = 30*3600*24
subjects = range(1,26)
subjects.remove(5)
subjects.remove(17)


for subject in subjects:
    print subject
    print 'reading data'
    truncated = open('%s/%d/%d_%dhz_truncated.csv' % (ROOT_PATH, subject, subject, FREQ)).readlines()
    duplicates = open('%s/%d/%d_%dhz_duplicates.csv' % (ROOT_PATH, subject, subject, FREQ)).readlines()
    se = open('%s/%d/%d_start_and_end.csv' % (ROOT_PATH, subject, subject)).readlines()
    print 'done reading data'

    raw_start = se[1].split(',')[1]
    raw_start_split = raw_start.split('/')
    second_day_start = '%d/%s/%s 0:00:00.000' % (int(raw_start_split[0])+1, raw_start_split[1], raw_start_split[2].split(' ')[0])
    first_day_len = dif_ticks(second_day_start, raw_start)
    
    day_end_ticks = [0, first_day_len]
    raw_end_tick = int(se[1].split(',')[4])
    while day_end_ticks[-1] + TICKS_PER_DAY < raw_end_tick:
        day_end_ticks.append(day_end_ticks[-1] + TICKS_PER_DAY)
    day_end_ticks.append(raw_end_tick)
    print day_end_ticks

    day = 0
    for i in xrange(1,len(truncated)):
        try:
            if int(truncated[i].split(',')[0]) > day_end_ticks[day]:
                day += 1
                day_stream = open('%s/%d/%d_30hz_truncated_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
                day_stream.write('Tick,Axis1,Axis2,Axis3\n')
            day_stream.write(truncated[i])
        except IndexError:
            print 'list index out of range'
            print i
            print truncated[i]
            exit()

    day = 0
    for i in xrange(1,len(duplicates)):
        line_split = duplicates[i].split(',')
        start_time = int(line_split[0])
        interval = int(line_split[4])
        if start_time > day_end_ticks[day]:
            day += 1
            day_stream = open('%s/%d/%d_30hz_duplicates_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
            day_stream.write('StartTime,Axis1,Axis2,Axis3,Interval\n')
            day_stream.write(duplicates[i])
        elif start_time + interval > day_end_ticks[day]:
            first_half_interval = day_end_ticks[day] - start_time + 1
            first_half = '%s,%s\n' % (','.join(line_split[:4]), first_half_interval)
            day_stream.write(first_half)
            second_half = '%s,%s,%s\n' % (day_end_ticks[day] + 1, ','.join(line_split[1:4]), interval - first_half_interval)
            day += 1
            day_stream = open('%s/%d/%d_30hz_duplicates_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
            day_stream.write(second_half)
        else:
            day_stream.write(duplicates[i])
