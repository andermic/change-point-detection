% Calculate control chart scores for each tick of each time series in NAMES_FILE

NAMES_FILE = 'uq_file_names';
KPRE = 30;
OUTPUT = '/nfs/guille/wong/users/andermic/Desktop/cpd/uq_30hz';

load(NAMES_FILE)
for path=(uq_file_names)'
    name = regexp(path', '/', 'split');
    name = name{1}{size(name{1},2)-1};
    disp(name);
    %scores = control_chart(KPRE, read_data(name'));
    scores = control_chart(KPRE, csvread(path{1},11,1));
    %assert(size(data,2) == 3);
    csvwrite(strcat(OUTPUT, '/cc_kpre', num2str(KPRE), '/scores_', name, '.csv'), scores);
end;
