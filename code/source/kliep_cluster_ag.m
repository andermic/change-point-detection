function [ fprs, ag_ars, ag_dts, ars_01, ars_05, ars_1, dts_01, dts_05, dts_1 ] = kliep_cluster_ag(kpre)
% Get scores, generate data for amot and roc curves

% Input -  kpre: The number of reference data points used to calculate the
%                given scores.

% Output - fprs: False positive rates.
%          ag_ars: Average accuracy rate for each false positive rate.
%          ad_dts: Average detection time for each false positive rate.
%          ars_01: All ars for fpr = 0.01.
%          ars_05: All ars for fpr = 0.05.
%          ars_1: All ars for fpr = 0.1.
%          dts_01: All dts for fpr = 0.01.
%          dts_05: All dts for fpr = 0.05.
%          dts_1: All dts for fpr = 0.1.

addpath('../../results/30hz/kliep_kpre300/');
addpath('../../../cpd_data/');
load score_names.mat
load data_names.mat

fpr_gran = 100;
ct_ars = zeros(fpr_gran+1,1);
ct_dts = zeros(fpr_gran+1,1);
ag_ars = zeros(fpr_gran+1,1);
ag_dts = zeros(fpr_gran+1,1);
ars_01 = [];
ars_05 = [];
ars_1 = [];
dts_01 = [];
dts_05 = [];
dts_1 = [];

for i = 1:size(score_names,1)
    i
    % Get scores and data for one of the 91 instances
    scores = csvread(score_names(i,:));
    data = csvread(data_names(i,:));
    
    % Calculate fpr, ar, and dt for this data instance
    true_changes = changes(data) - kpre;
    [fprs, ars, dts] = amot_roc(scores, true_changes);
    fprs = round(fprs*fpr_gran)+1;
    
    % Store ars and dts for 0.01, 0.05, and 0.1
    ars_01 = [ars_01; ars(fprs==2)];
    ars_05 = [ars_05; ars(fprs==6)];
    ars_1 = [ars_1; ars(fprs==11)];
    dts_01 = [dts_01; dts(fprs==2)];
    dts_05 = [dts_05; dts(fprs==6)];
    dts_1 = [dts_1; dts(fprs==11)];
    
    % Keep an online average of ars and dts across all data instances
    for j = 1:size(fprs,1)
        fpr = fprs(j);
        ag_ars(fpr) = ag_ars(fpr) * ct_ars(fpr) + ars(j);
        ct_ars(fpr) = ct_ars(fpr) + 1;
        ag_ars(fpr) = ag_ars(fpr) / ct_ars(fpr);
        ag_dts(fpr) = ag_dts(fpr) * ct_dts(fpr) + dts(j);
        ct_dts(fpr) = ct_dts(fpr) + 1;
        ag_dts(fpr) = ag_dts(fpr) / ct_dts(fpr);
    end
end
fprs = (0:(1/fpr_gran):1)';
