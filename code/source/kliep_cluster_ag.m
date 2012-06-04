function [ fprs, ag_ars, ag_dts ] = kliep_cluster_ag(kpre)
% Get scores, generate amot and roc curves

addpath('../../results/30hz/kliep_kpre300/')
addpath('../../../cpd_data');
load score_names.mat
load data_names.mat

fpr_gran = 100;
ct_ars = zeros(fpr_gran+1,1);
ct_dts = zeros(fpr_gran+1,1);
ag_ars = zeros(fpr_gran+1,1);
ag_dts = zeros(fpr_gran+1,1);

for i = 1:48
    i
    scores = csvread(score_names(i,:));
    data = csvread(data_names(i,:));
    true_changes = changes(data) - kpre;
    [fprs, ars, dts] = amot_roc(scores, true_changes);
    fprs = round(fprs*fpr_gran)+1;
    for j = fprs'
        ag_ars(j) = ag_ars(j) * ct_ars(j) + ars(j);
        ct_ars(j) = ct_ars(j) + 1;
        ag_ars(j) = ag_ars(j) / ct_ars(j);
        ag_dts(j) = ag_dts(j) * ct_dts(j) + dts(j);
        ct_dts(j) = ct_dts(j) + 1;
        ag_dts(j) = ag_dts(j) / ct_dts(j);
    end
end
fprs = (0:(1/fpr_gran):1)';