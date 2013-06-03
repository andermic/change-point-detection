from os import mkdir, listdir

def make_path(path):
    folders = path.split('/')[:-1]  
    if path[0] != '/':
        folders = ['.'] + folders

    cur_folder = ''
    for i in range(1, len(folders)):
        cur_folder += folders[i-1] + '/'
        if folders[i] not in listdir(cur_folder):
            mkdir(cur_folder + folders[i])
