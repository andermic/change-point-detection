#! /usr/bin/python

from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'

names = [i.strip() for i in open('%s/30hz_file_names.csv' % ROOT_PATH, 'r').readlines()]
buffer = ''
start_line = ['', '', '', '']
for name in names:
    input = open(name, 'r')
    folder = name.split('/')[-2]
    truncated = open('%s/%s/%s_30hz_truncated.csv' % (ROOT_PATH, folder, folder), 'w')
    duplicates = open('%s/%s/%s_30hz_duplicates.csv' % (ROOT_PATH, folder, folder), 'w')
    duplicates.write('StartTime,Axis1,Axis2,Axis3,Interval\n')
    contig_duplicate_count = 0
    for i in range(11):
        input.readline()

    count = 0
    while True:
        count += 1
        if count % 10000 == 0:
            print count
        line = input.readline()
        line_split = line.split(',')
        if line_split[0][-3:] == '967':
            buffer += line
            continue
        if line == '':
            break
        if line_split[-3:] == start_line[-3:]:
            buffer += line
            contig_duplicate_count += 1
        else:
            if contig_duplicate_count >= 29:
                dif = dif_ticks(line_split[0], start_line[0])
                duplicates.write('%s,%d\n' % (','.join(start_line).strip(), dif))
                buffer = line
            else:
                truncated.write(buffer)
                buffer = line
                start_line = line_split
            contig_duplicate_count = 0
    truncated.close()
    duplicates.close()
