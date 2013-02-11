#! /usr/bin/python

import re

log = open('/nfs/guille/u2/a/andermic/scratch/workspace/GeneralExperimentFramework/gef.log', 'r').readlines()

executed = len([line for line in log if 'Rscript' in line])
executors = 0
for line in log:
    search = re.search(' [0-9]+ executors created', line)
    if search == None:
        continue
    executors += int(search.group().split(' ')[1])
print '%d/%d jobs executed' % (executed, executors)
