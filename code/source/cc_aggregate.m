function [ fprs, ag_ars, ag_dts ] = cc_aggregate(kpre)
% For each data instance, run a control chart and aggregate results

% Calculate amot and roc curves for each data instance
addpath('../../../cpd_data');
load data_names.mat;

fpr_gran = 100;
ct_ars = zeros(fpr_gran+1,1);
ct_dts = zeros(fpr_gran+1,1);
ag_ars = zeros(fpr_gran+1,1);
ag_dts = zeros(fpr_gran+1,1);

for i = 1:size(data_names,1)
    i
    data = csvread(data_names(i,:));
    cc_scores = control_chart(kpre, data);
    true_changes = changes(data) - kpre;
    [fprs, ars, dts] = amot_roc(cc_scores, true_changes);
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