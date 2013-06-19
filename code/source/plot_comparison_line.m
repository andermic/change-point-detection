ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/results/30hz';
ROOT_OUTPUT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/thesis/figures';
DATASETS = {{'OSU_YR4_Hip_30Hz.ws120.7cls','uq_30Hz_day2','uq_30Hz_day3'},{'OSU_YR4_Hip_30Hz.HMM.7cls','uq_30Hz_day2.HMM','uq_30Hz_day3.HMM'}};
DATASET_STRS = {' OSU Hip', ' LiME Day 1', ' LiME Day 2'};
DATASET_FILE_STRS = {'osu', 'lime1', 'lime2'};
CLASS_ALGS = {'dt', 'svm', 'nnet'};
CLASS_ALG_STRS = {' Decision Tree', ' SVM', ' Neural Network'};
CPD_ALGS = {'cc', 'kliep'};
METRICS = {' Accuracy', ' Detection Time'};

FPRS = [0.0005 0.001 0.0017 0.0019 0.0021 0.0024 0.0028 0.0033 0.005 0.01];
FPRS2 = [0.0005 0.001 0.005 0.01];
WSES = 10:2:20;
WSES_STR = {'0.033' '0.028' '0.024' '0.021' '0.019' '0.017'};

TEST_WINDOW_SIZE = 1;
MARKERSIZE = 3;

addpath(ROOT_INPUT_FOLDER);
for idata = 1:3
  for iclass=1:3
    input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', CLASS_ALGS{iclass}, '_cc_results.csv');
    cc = csvread(input_file, 1);
    input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', CLASS_ALGS{iclass}, '_kliep_results.csv');
    kliep = csvread(input_file, 1);
    kliep(:,4) = kliep(:,4) + TEST_WINDOW_SIZE;
    input_file = strcat(ROOT_INPUT_FOLDER, '/hmm/', DATASETS{2}{idata}, '/', CLASS_ALGS{iclass}, '_results.csv');
    hmm = csvread(input_file, 1);
    input_file = strcat(ROOT_INPUT_FOLDER, '/nohmm/', DATASETS{2}{idata}, '/', CLASS_ALGS{iclass}, '_results.csv');
    nohmm = csvread(input_file, 1);

    cc_indices = [];
    kliep_indices = [];
    hmm_indices = [];
    nohmm_indices = [];
    if idata == 1
        fprs = FPRS;
    else
        fprs = FPRS2;
    end
    for fpr = fprs
        cc_indices = [cc_indices, find(cc(:,1)==fpr)];
        kliep_indices = [kliep_indices, find(kliep(:,1)==fpr)];
    end
    for ws = WSES
        hmm_indices = [hmm_indices, find(hmm(:,1)==ws)];
        nohmm_indices = [nohmm_indices, find(nohmm(:,1)==ws)];
    end

    cc = cc(cc_indices, :);
    kliep = kliep(kliep_indices, :);
    hmm = hmm(hmm_indices, :);
    nohmm = nohmm(nohmm_indices, :);
    cc(:,1) = cc(:,1) * 10;
    kliep(:,1) = kliep(:,1) * 10;
    hmm(:,1) = 1./(3*hmm(:,1));
    nohmm(:,1) = 1./(3*nohmm(:,1));

    cc(:,2:3) = cc(:,2:3) * 100;
    kliep(:,2:3) = kliep(:,2:3) * 100;
    nohmm(:,2:3) = nohmm(:,2:3) * 100;

    for imet = 1:2
        best_cpd = zeros(size(fprs,2), 3);
        best_met = cc(:,imet*2)*(3-2*imet) > kliep(:,imet*2)*(3-2*imet);
        for i = 1:size(fprs,2)
            if best_met(i)
                best_cpd(i,:) = cc(i,[1 (imet*2) (imet*2+1)]);
            else
                best_cpd(i,:) = kliep(i,[1 (imet*2) (imet*2+1)]);
            end
        end
        hmm(:,2:3) = hmm(:,(imet*2):(imet*2+1));
        nohmm(:,2:3) = nohmm(:,(imet*2):(imet*2+1));
 
        lbls = [best_cpd(:,1); hmm(:,1); nohmm(:,1)];
        vals = [best_cpd(:,2); hmm(:,2); nohmm(:,2)];
        stds = [best_cpd(:,3); hmm(:,3); nohmm(:,3)];
        %temp = sortrows([lbls, (1:(size(FPRS,2)+size(WSES,2)))']);
        %temp = sortrows([temp(:,2), (1:(size(FPRS,2)+size(WSES,2)))']);
        %indices = temp(:,2);
        %cpd_indices = indices(1:size(FPRS,2));
        %hmm_indices = indices((size(FPRS,2)+1):size(indices,1));
  
        f = figure();
        hold on;
        plot(best_cpd(:,1), best_cpd(:,2), '--');
        plot(hmm(:,1), hmm(:,2), 'color', 'r');
        plot(nohmm(:,1), nohmm(:,2), 'linestyle', '-.', 'color', 'g');
        plot(best_cpd(:,1), best_cpd(:,2), 'o', 'markersize', MARKERSIZE);
        plot(hmm(:,1), hmm(:,2), 'o', 'color', 'r', 'markersize', MARKERSIZE);
        plot(nohmm(:,1), nohmm(:,2), 'o', 'color', 'g', 'markersize', MARKERSIZE);
        set(gca, 'fontsize', 14);
        %line(best_cpd(:,1), best_cpd(:,2));
        %line(hmm(:,1), hmm(:,2), 'color', 'r');
        
        if imet == 1
            ylabel(strcat(METRICS{imet}, ' (%)'));
        else
            ylabel(strcat(METRICS{imet}, ' (s)'));
        end
        
        %errorbar(best_cpd(:,1), best_cpd(:,2), best_cpd(:,3) * 1.96 / sqrt(30), 'linestyle', 'none');
        %errorbar(hmm(:,1), hmm(:,2), hmm(:,3) * 1.96 / sqrt(10), 'r', 'linestyle', 'none');

        labels = 0.1:0.1:1;
        %set(gca, 'XTick', 1:(size(FPRS,2)+size(WSES,2)), 'XTickLabel', labels(indices));
        xlabel('False Positives / s');
        if imet == 1
            ylabel(strcat(METRICS{imet}, ' (%)'));
            ym = 100;
        else
            ylabel(strcat(METRICS{imet}, ' (s)'));
            ym = max(max(vals+stds*1.96));
        end
        axis([0 .105 0 ym]);
        subfig = num2str(idata + 6);
        subsubfig = num2str((iclass - 1) * 2 + imet);
        %title(strcat(DATASET_STRS{idata}, ':', CLASS_ALG_STRS{iclass}, METRICS(imet)));
        title(strcat('Figure 4.', subfig, '.', subsubfig, ':', CLASS_ALG_STRS{iclass}, METRICS(imet)));
        legend('Top-Down', 'Bottom-Up', 'Bottom-Up without HMM');
        set(gca,'Box','on');

        print(gcf, '-djpeg', '-r100', strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_', CLASS_ALGS{iclass}, '_cpd_hmm_compare_', lower(METRICS{imet}(2:4)), '_line.jpg'));
        %saveas(gcf, strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_', CLASS_ALGS{iclass}, '_cpd_hmm_compare_', lower(METRICS{imet}(2:4)), '_line.eps'), 'epsc');
        hold off;
    end
  end
end
