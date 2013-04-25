#! /usr/bin/python

# Split already truncated data into single day sections

from datetime import datetime, timedelta
from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed'
FREQ = 30
DAY_LEN = 30*3600*24
subjects = range(1,26)
subjects.remove(5)
subjects.remove(17)


for subject in subjects:
    print subject
    print 'reading data'
    truncated = open('%s/%d/%d_%dhz_truncated.csv' % (ROOT_PATH, subject, subject, FREQ)).readlines()
    duplicates = open('%s/%d/%d_%dhz_duplicates.csv' % (ROOT_PATH, subject, subject, FREQ)).readlines()
    events = open('%s/%d/%d_events.csv' % (ROOT_PATH, subject, subject)).readlines()
    se = open('%s/%d/%d_start_and_end.csv' % (ROOT_PATH, subject, subject)).readlines()
    print 'done reading data'

    raw_start = se[1].split(',')[1]
    raw_start_split = raw_start.split('/')
    second_day_start = '%d/%s/%s 0:00:00.000' % (int(raw_start_split[0])+1, raw_start_split[1], raw_start_split[2].split(' ')[0])
    first_day_len = dif_ticks(second_day_start, raw_start)
    
    day_end_ticks = [0, first_day_len]
    raw_end_tick = int(se[1].split(',')[4])
    while day_end_ticks[-1] + DAY_LEN < raw_end_tick:
        day_end_ticks.append(day_end_ticks[-1] + DAY_LEN)
    day_end_ticks.append(raw_end_tick)
    print day_end_ticks

    """
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
    """

    day = 0
    for i in xrange(1,len(duplicates)):
        line_split = duplicates[i].split(',')
        start_tick = int(line_split[0])
    
        interval = int(line_split[4])
        if start_tick > day_end_ticks[day]:
            day += 1
            day_stream = open('%s/%d/%d_30hz_duplicates_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
            day_stream.write('StartTime,Axis1,Axis2,Axis3,Interval\n')
            day_stream.write(duplicates[i])
        elif start_tick + interval > day_end_ticks[day]:
            cur_interval = day_end_ticks[day] - start_tick + 1
            first_part = '%s,%s\n' % (','.join(line_split[:4]), cur_interval)
            day_stream.write(first_part)
            second_half = '%s,%s,%s\n' % (day_end_ticks[day] + 1, ','.join(line_split[1:4]), interval - cur_interval)
            day += 1
            day_stream = open('%s/%d/%d_30hz_duplicates_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
            day_stream.write('StartTime,Axis1,Axis2,Axis3,Interval\n')
            day_stream.write(second_half)
        else:
            day_stream.write(duplicates[i])

    """
    day = 0
    for i in xrange(1,len(events)):
        line_split = events[i].split(',')
        time = line_split[0].split(' ')
        start_tick = int(line_split[1])
        interval = int(round(30*float(line_split[2]))) if i == len(events)-1 \
         else int(events[i+1].split(',')[1]) - start_tick
        done = False

        while start_tick + interval > day_end_ticks[day] + 1:
            print interval
            print start_tick
            print day
            print day_end_ticks
            print
            cur_interval = day_end_ticks[day] - start_tick + 1
            if cur_interval > 0:
                cur_section = '%s,%d,%d,%s' % (' '.join(time), start_tick, cur_interval, ','.join(line_split[3:]))
                if day != 0:
                    day_stream.write(cur_section)
            else:
                cur_interval = 0

            day += 1
            if day == len(day_end_ticks):
                done = True
                break
            day_stream = open('%s/%d/%d_events_day%d.csv' % (ROOT_PATH, subject, subject, day), 'w')
            day_stream.write('Time,StartTick,Interval,ActivityCode,CumulativeStepCount,ActivityScore\n')
            if day != 1:
                dt = datetime.strptime(time[0], '%d/%m/%Y')
                dt += timedelta(days = 1)
                time = [dt.strftime('%d/%m/%Y'), '00:00:00.000']
                start_tick = day_end_ticks[day-1] + 1
                interval -= cur_interval
        if done:
            break
        day_stream.write('%s,%d,%d,%s' % (' '.join(time), start_tick, interval, ','.join(line_split[3:])))
    """
