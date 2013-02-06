from os import listdir
from os import system

system('mkdir temp')
for file in listdir('.'):
    if file == 'blah.py' or file == 'temp':
        continue
    data = open(file, 'r').read().split(',')
    #print file
    #print 'ChangePointPredictions' + '\n' + '\n'.join(data)
    open('temp/' + file, 'w').write('ChangePointPredictions' + '\n' + '\n'.join(data))
