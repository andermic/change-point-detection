#! /usr/bin/python

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'
#FOLDERS = ['1']
FOLDERS = ['1', '2', '3', '4', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '18', '19', '20', '21', '22', '23', '24', '25']

counts = [0,0,0]
for folder in FOLDERS:
    lines = open('%s/%s/%s_events.csv' % (ROOT_PATH, folder, folder), 'r').readlines()[1:]
    for line in lines:
        spl_line = line.split(',')
        interval = float(spl_line[2])
        activity = int(spl_line[3])
        counts[activity] += interval

for i in range(3):
    print 'Activity %d - %d/%d = %f%%' % (i, counts[i], sum(counts), counts[i]/sum(counts))
