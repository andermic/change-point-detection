#! /usr/bin/python

# THIS CODE SHOULD BE OBSOLETE

# Convert the timestamp format of the compressed data into a tick value format
#  so that it is easier to manipulate


from dif_ticks import dif_ticks

ROOT_PATH = '/nfs/guille/wong/wonglab3/obesity/freeliving/UQ/processed'
FOLDERS = ['1', '2', '3', '4', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '18', '19', '20', '21', '22', '23', '24', '25']
for folder in FOLDERS:
    print folder
    start = open('%s/%s/%s_start_and_end.csv' % (ROOT_PATH, folder, folder)).readlines()[1].split(',')[1]

    truncated = open('%s/%s/%s_30hz_truncated2.csv' % (ROOT_PATH, folder, folder), 'w')
    truncated.write('Tick,Axis1,Axis2,Axis3\n')
    for line in open('%s/%s/%s_30hz_truncated.csv' % (ROOT_PATH, folder, folder), 'r').readlines()[1:]:
        line_split = line.split(',')
        line_split[0] = str(dif_ticks(line_split[0], start)) + 1
        truncated.write(','.join(line_split))
    truncated.close()

    duplicates = open('%s/%s/%s_30hz_duplicates2.csv' % (ROOT_PATH, folder, folder), 'w')
    duplicates.write('StartTick,Axis1,Axis2,Axis3,Interval\n')
    for line in open('%s/%s/%s_30hz_duplicates.csv' % (ROOT_PATH, folder, folder), 'r').readlines()[1:]:
        line_split = line.split(',')
        line_split[0] = str(dif_ticks(line_split[0], start)) + 1
        duplicates.write(','.join(line_split))
