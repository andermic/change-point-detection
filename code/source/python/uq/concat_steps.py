#! /usr/bin/python

ROOT_PATH = '/nfs/guille/wong/users/andermic/uq/processed'
folders = range(1,26)
folders.remove(5)
folders.remove(17)

for f in folders:
    lines = open('%s/%d/%d_events_steps.csv' % (ROOT_PATH, f, f), 'r').readlines()

    out = open('%s/%d/%d_events.csv' % (ROOT_PATH, f, f), 'w')
    out.write('Time,StartTick,Interval,ActivityCode,CumulativeStepCount\n')
    concat_line = lines[1].split(',')[:-1]

    for line in lines[2:]:
        spl_line = line.split(',')[:-1]
        if spl_line[3] != concat_line[3]:
            out.write(','.join(concat_line) + '\n')
            concat_line = spl_line
        else:
            concat_line[2] = '%.2f' % (float(concat_line[2]) + float(spl_line[2]))
            concat_line[4] = spl_line[4]
    out.write(','.join(concat_line) + '\n')
