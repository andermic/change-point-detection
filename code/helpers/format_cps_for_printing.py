from os import listdir

l = ''
for file in listdir('.'):
    if file[0] == 's':
        continue
    l += ','.join([i.strip() for i in (open(file,'r').readlines()[1:])]) + '\n'

open('saved.txt','w').write(l)
