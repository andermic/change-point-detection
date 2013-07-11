function [ fprs, ag_ars, ag_dts, cc_ars_01, cc_ars_05, cc_ars_1, cc_dts_01, cc_dts_05, cc_dts_1 ] = cc_aggregate(kpre, fpr_split)
% For each data instance, run a control chart and aggregate results

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


addpath('../../../cpd_data');
load data_names.mat;

fpr_gran = 100;
ct_ars = zeros(fpr_gran+1,1);
ct_dts = zeros(fpr_gran+1,1);
ag_ars = zeros(fpr_gran+1,1);
ag_dts = zeros(fpr_gran+1,1);
cc_ars_01 = [];
cc_ars_05 = [];
cc_ars_1 = [];
cc_dts_01 = [];
cc_dts_05 = [];
cc_dts_1 = [];
PC_FOLDER_NAME = 'predicted_changes';

if fpr_split ~= 0
    mkdir(PC_FOLDER_NAME);
    mkdir(strcat(PC_FOLDER_NAME,'/',num2str(fpr_split)));
end

% Calculate amot and roc curves for each data instance
for i = 1:size(data_names,1)
    i
    % Get scores and data for one of the 91 instances
    data = csvread(data_names(i,:));
    cc_scores = control_chart(kpre, data);
    
    % Calculate false positive rates, accuracy rates, and detection times for this data instance
    true_changes = changes(data) - kpre;
    [fprs, ars, dts, predictions] = amot_roc(cc_scores, true_changes, kpre, fpr_split);
    fprs = round(fprs*fpr_gran)+1;
    
    % Store accuracy rates and detection times for 0.01, 0.05, and 0.1 false positive rates
    cc_ars_01 = [cc_ars_01; ars(fprs==2)];
    cc_ars_05 = [cc_ars_05; ars(fprs==6)];
    cc_ars_1 = [cc_ars_1; ars(fprs==11)];
    cc_dts_01 = [cc_dts_01; dts(fprs==2)];
    cc_dts_05 = [cc_dts_05; dts(fprs==6)];
    cc_dts_1 = [cc_dts_1; dts(fprs==11)];
    
    for j = 1:size(fprs,1)
        fpr = fprs(j);
        ag_ars(fpr) = ag_ars(fpr) * ct_ars(fpr) + ars(j);
        ct_ars(fpr) = ct_ars(fpr) + 1;
        ag_ars(fpr) = ag_ars(fpr) / ct_ars(fpr);
        ag_dts(fpr) = ag_dts(fpr) * ct_dts(fpr) + dts(j);
        ct_dts(fpr) = ct_dts(fpr) + 1;
        ag_dts(fpr) = ag_dts(fpr) / ct_dts(fpr);
    end

    % Write predicted changes for this data instance to file
    if fpr_split ~= 0
        filepath = strcat(PC_FOLDER_NAME,'/',num2str(fpr_split),'/',data_names(i,:));
        fid = fopen(filepath, 'w');
        fprintf(fid, 'ChangePointPredictions\n')
        for pred = predictions
            fprintf(fid, '%d\n', pred)
        end
        fclose(fid);
    end
end
fprs = (0:(1/fpr_gran):1)';
