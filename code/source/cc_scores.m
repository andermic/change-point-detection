NAMES_FILE = 'uq_file_names';
KPRE = 30;
OUTPUT = '/nfs/guille/wong/users/andermic/Desktop/cpd/uq_30hz';

load(NAMES_FILE)
for name=(uq_file_names)'
    name'
    %scores = control_chart(KPRE, read_data(name'));
    scores = control_chart(KPRE, csvread(name',11,1));
    assert(size(data,2) == 3);
    csvwrite(strcat(OUTPUT, '/cc_kpre', num2str(KPRE), '/scores.', name'), scores);
end;