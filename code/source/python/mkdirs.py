from os import mkdir, listdir

def mkdirs(path):
    folders = path.split('/')[:-1]  
    if path[0] != '/':
        folders = ['.'] + folders

    cur_folder = ''
    for i in range(1, len(folders)):
        cur_folder += folders[i-1] + '/'
        print cur_folder
        print folders[i]
        if folders[i] not in listdir(cur_folder):
            mkdir(cur_folder + folders[i])
            print 'made ' + folders[i]
