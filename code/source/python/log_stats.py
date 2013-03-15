#! /usr/bin/python

import re
from os import system

log = open('/nfs/guille/wong/users/andermic/scratch/workspace/GeneralExperimentFramework/gef.log', 'r').readlines()

times = []
for line in log:
    search = re.search('Time elapsed: [0-9]+', line)
    if search == None:
        continue
    times.append(int(search.group().split(' ')[2]))
times = [i / 1000. for i in times]

system('clear')

print 'Times:',
for time in times:
    print '%.2f' % time,

if len(times) == 0:
    print '<none>'
    exit()

print '\n\nAverage time: %.2fs' % (sum(times)/len(times))
print 'Min time: %.2fs' % min(times)
print 'Max time: %.2fs' % max(times)
