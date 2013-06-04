ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/results/30hz';
ROOT_OUTPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/thesis/figures';
DATASETS = {{'OSU_YR4_Hip_30Hz.ws120.7cls','uq_30Hz_day2','uq_30Hz_day3'},{'OSU_YR4_Hip_30Hz.HMM.7cls','uq_30Hz_day2.HMM','uq_30Hz_day3.HMM'}};
DATASET_STRS = {' OSU Hip', ' LiME Day 1', ' LiME Day 2'};
DATASET_FILE_STRS = {'osu', 'lime1', 'lime2'};
CLASS_ALGS = {'dt', 'svm', 'nnet'};
CLASS_ALG_STRS = {' Decision Tree', ' SVM', ' Neural Network'};
CPD_ALGS = {'cc', 'kliep'};
METRICS = {' Accuracy', ' Detection Time'};

FPRS_STR = {'0.005' '0.01' '0.05' '0.1'};
WSES = 10:2:20;
WSES_STR = {'0.033' '0.028' '0.024' '0.021' '0.019' '0.017'};

REF_WINDOW_SIZE = 1;

addpath(ROOT_INPUT_FOLDER);
for imet = 1:2
    data = zeros(3,size(FPRS,2)+size(WSES,2),3);
    for idata = 1:3
        input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', 'dt_cc_results.csv');
        cc = csvread(input_file, 1);
        input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', 'dt_kliep_results.csv');
        kliep = csvread(input_file, 1);
        kliep(:,4) = kliep(:,4) + REF_WINDOW_SIZE;
        input_file = strcat(ROOT_INPUT_FOLDER, '/hmm/', DATASETS{2}{idata}, '/', 'dt_results.csv');
        hmm = csvread(input_file, 1);
        
        cc_indices = [];
        kliep_indices = [];
        hmm_indices = [];
        for fpr = FPRS
            cc_indices = [cc_indices, find(cc(:,1)==fpr)];
            kliep_indices = [kliep_indices, find(kliep(:,1)==fpr)];
        end
        for ws = WSES
            hmm_indices = [hmm_indices, find(hmm(:,1)==ws)];
        end
        
        cc = cc(cc_indices, :);
        kliep = kliep(kliep_indices, :);
        hmm = hmm(hmm_indices, :);
        cc(:,1) = cc(:,1) * 10;
        kliep(:,1) = kliep(:,1) * 10;
        hmm(:,1) = 1./(3*hmm(:,1));
        if imet == 1
            cc(:,2:3) = cc(:,2:3) * 100;
            kliep(:,2:3) = kliep(:,2:3) * 100;
            hmm(:,2:3) = hmm(:,2:3) * 100;
            ylabel(strcat(METRICS{imet}, ' (%)'));
        else
            ylabel(strcat(METRICS{imet}, ' (s)'));
        end
        
        best_met = cc(:,imet*2)*(3-2*imet) > kliep(:,imet*2)*(3-2*imet);
        for i = 1:size(FPRS,2)
            if best_met(i)
                data(idata,i,:) = cc(i,[1 (imet*2) (imet*2+1)]);
            else
                data(idata,i,:) = kliep(i,[1 (imet*2) (imet*2+1)]);
            end
        end
        data(idata,(size(FPRS,2)+1):(size(FPRS,2)+size(WSES,2)),:) = hmm(:,[1 (imet*2) (imet*2+1)]);
        
        data(idata,:,:);
    end
    data(:,1:size(FPRS,2),3) = data(:,1:size(FPRS,2),3) / sqrt(30);
    data(:,(size(FPRS,2)+1):(size(FPRS,2)+size(WSES,2)),3) = data(:,(size(FPRS,2)+1):(size(FPRS,2)+size(WSES,2)),3) / sqrt(10);
    lbls = data(:,:,1)';
    vals = data(:,:,2)';
    size(vals);
    vals = sortrows([lbls(:,1), vals, (1:(size(FPRS,2)+size(WSES,2)))']);
    indices = vals(:,5);
    vals = vals(:,2:4);
    stds = data(:,:,3)';
    f = bar(vals);
    set(f(:,1), 'facecolor', [0.6 0.6 0.6]);
    set(f(:,2), 'facecolor', [0.9 0.9 0.9]);
    set(f(:,3), 'facecolor', [1 1 1]);
    hold on;
    errorbar([(1:(size(FPRS,2)+size(WSES,2)))'-.223, (1:(size(FPRS,2)+size(WSES,2)))', (1:(size(FPRS,2)+size(WSES,2)))'+.223], vals, stds(indices,:) * 1.96, 'black', 'linestyle', 'none')
    labels = [FPRS_STR WSES_STR];
    set(gca, 'XTick', 1:(size(FPRS,2)+size(WSES,2)), 'XTickLabel', labels(indices));
    xlabel('False Positives / s');
    ym = max(max(vals+stds*1.96))*1.2;
    if imet == 1
        ylabel(strcat(METRICS{imet}, ' (%)'));
    else
        ylabel(strcat(METRICS{imet}, ' (s)'));
    end
    axis([.5 size(FPRS,2)+size(WSES,2)+.5 0 ym]);
    title(strcat('Change-Point Detection vs. HMM: Decision Tree', METRICS(imet)));
    legend('OSU Hip', 'LiME Day 1', 'LiME Day 2');
    saveas(gcf, strcat(ROOT_OUTPUT_FOLDER, '/', 'cpd_hmm_compare_', lower(METRICS{imet}(2:4)), '.eps'));
    hold off;
end