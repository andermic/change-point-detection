#! /usr/bin/python

# Compress UQ dataset, since most of it contains contiguous ticks that have the
#  same accelerometer values

from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'

names = [i.strip() for i in open('%s/30hz_file_names.csv' % ROOT_PATH, 'r').readlines()]
buffer = ''
start_line = ['', '', '', '']
for name in names[1:]:
    input = open(name, 'r')
    folder = name.split('/')[-2]
    start = open('%s/%s/%s_start_and_end.csv' % (ROOT_PATH, folder, folder)).readlines()[1].split(',')[1]
    truncated = open('%s/%s/%s_30hz_truncated2.csv' % (ROOT_PATH, folder, folder), 'w')
    truncated.write('Tick,Axis1,Axis2,Axis3\n')
    duplicates = open('%s/%s/%s_30hz_duplicates2.csv' % (ROOT_PATH, folder, folder), 'w')
    duplicates.write('StartTime,Axis1,Axis2,Axis3,Interval\n')
    contig_duplicate_count = 0
    for i in range(11):
        input.readline()

    count = 0
    while True:
        count += 1
        if count % 10000 == 0:
            print name, count
        line = input.readline()
        if line == '':
            truncated.write(buffer)
            break
        line_split = line.split(',')
        line_tick = dif_ticks(line_split[0], start) + 1
        line = '%d,%s' % (line_tick, ','.join(line_split[1:]))
        if line_split[0][-3:] == '967':
            buffer += line 
            continue
        if line_split[-3:] == start_line[-3:]:
            buffer += line
            contig_duplicate_count += 1
        else:
            if contig_duplicate_count >= 29:
                interval_len = dif_ticks(line_split[0], start_line[0])
                interval_start = dif_ticks(start_line[0], start) + 1
                duplicates.write('%d,%s,%d\n' % (interval_start, ','.join(start_line[1:]).strip(), interval_len))
                buffer = line
            else:
                truncated.write(buffer)
                buffer = line
                start_line = line_split
            contig_duplicate_count = 0
    truncated.close()
    duplicates.close()
