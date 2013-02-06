load('data_names')
for name=(data_names)'
    name'
    scores = control_chart(300, read_data(name'));
    csvwrite(strcat('../../results/30hz/cc_kpre300/scores.', name'), scores);
end