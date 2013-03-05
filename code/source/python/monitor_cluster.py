#! /usr/bin/python

from os import system
from time import sleep
from sys import argv

FILE_NAME = 'monitor_cluster.tmp'
try:
    sleep_duration = int(argv[1])
except:
    sleep_duration = 30

while(True):
    system('qstat | grep andermic > ' + FILE_NAME)
    lines = open(FILE_NAME).readlines()
    print 'Jobs queued and waiting: %d' % len([job for job in lines if ' qw ' in job])
    print 'Jobs running: %d' % len([job for job in lines if ' r ' in job])
    print 'Jobs in an error state: %d' % len([job for job in lines if ' E' in job])
    print
    sleep(sleep_duration)
