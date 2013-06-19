ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/results/30hz';
ROOT_OUTPUT_FOLDER = '/nfs/stak/students/a/andermic/Windows.Documents/Desktop/change-point-detection/thesis/figures';
DATASETS = {{'OSU_YR4_Hip_30Hz.ws120.7cls','uq_30Hz_day2','uq_30Hz_day3'},{'OSU_YR4_Hip_30Hz.HMM.7cls','uq_30Hz_day2.HMM','uq_30Hz_day3.HMM'}};
DATASET_STRS = {' OSU Hip', ' LiME Day 1', ' LiME Day 2'};
DATASET_FILE_STRS = {'osu', 'lime1', 'lime2'};
CLASS_ALGS = {'dt', 'svm', 'nnet'};
CLASS_ALG_STRS = {' Decision Tree', ' SVM', ' Neural Network'};
CPD_ALGS = {'cc', 'kliep'};
METRICS = {' Accuracy', ' Detection Time'};

WSES = 10:2:20;
MARKERSIZE = 3;

addpath(ROOT_INPUT_FOLDER);
for idata = 1:3
  for iclass=1:3
    input_file = strcat(ROOT_INPUT_FOLDER, '/nohmm/', DATASETS{2}{idata}, '/', CLASS_ALGS{iclass}, '_results.csv');
    nohmm = csvread(input_file, 1);
    input_file = strcat(ROOT_INPUT_FOLDER, '/hmm/', DATASETS{2}{idata}, '/', CLASS_ALGS{iclass}, '_results.csv');
    hmm = csvread(input_file, 1);

    nohmm_indices = [];
    hmm_indices = [];
    for ws = WSES
        nohmm_indices = [hmm_indices, find(hmm(:,1)==ws)];
        hmm_indices = [hmm_indices, find(hmm(:,1)==ws)];
    end
    
    nohmm = nohmm(nohmm_indices, :);
    hmm = hmm(hmm_indices, :);

    nohmm(:,2:3) = nohmm(:,2:3) * 100;
    hmm(:,2:3) = hmm(:,2:3) * 100;

    for imet = 1:2
        nohmm(:,2:3) = nohmm(:,(imet*2):(imet*2+1));
        hmm(:,2:3) = hmm(:,(imet*2):(imet*2+1));
        
        lbls = [hmm(:,1), nohmm(:,1)];
        vals = [hmm(:,2), nohmm(:,2)];
        stds = [hmm(:,3), nohmm(:,3)];
        %temp = sortrows([lbls, (1:(size(FPRS,2)+size(WSES,2)))']);
        %temp = sortrows([temp(:,2), (1:(size(FPRS,2)+size(WSES,2)))']);
        %indices = temp(:,2);
        %cpd_indices = indices(1:size(FPRS,2));
        %hmm_indices = indices((size(FPRS,2)+1):size(indices,1));
  
        f = bar(lbls, vals);
        set(f(:,1), 'facecolor', [0.9 0.9 0.9]);
        set(f(:,2), 'facecolor', [1 1 1]);
        hold on;
        %plot(hmm(:,1), hmm(:,2), '--');
        %plot(nohmm(:,1), nohmm(:,2), 'color', 'r');
        %plot(hmm(:,1), hmm(:,2), 'o', 'markersize', MARKERSIZE);
        %plot(nohmm(:,1), nohmm(:,2), 'o', 'color', 'r', 'markersize', MARKERSIZE);
        
        %plot(hmm(:,1), hmm(:,2), '--');
        %plot(nohmm(:,1), nohmm(:,2), 'color', 'r');
        %plot(hmm(:,1), hmm(:,2), 'o', 'markersize', MARKERSIZE);
        %plot(nohmm(:,1), nohmm(:,2), 'o', 'color', 'r', 'markersize', MARKERSIZE);
        
        if imet == 1
            ylabel(strcat(METRICS{imet}, ' (%)'));
        else
            ylabel(strcat(METRICS{imet}, ' (s)'));
        end
        
        errorbar(hmm(:,1) - 0.26, hmm(:,2), hmm(:,3) * 1.96 / sqrt(10), 'black', 'linestyle', 'none');
        errorbar(nohmm(:,1) + 0.26, nohmm(:,2), nohmm(:,3) * 1.96 / sqrt(10), 'black', 'linestyle', 'none');
        labels = 0.1:0.1:1;
        %set(gca, 'XTick', 1:(size(FPRS,2)+size(WSES,2)), 'XTickLabel', labels(indices));
        xlabel('Window Sizes (Seconds)');
        if imet == 1
            ylabel(strcat(METRICS{imet}, ' (%)'));
            ym = 100;
        else
            ylabel(strcat(METRICS{imet}, ' (s)'));
            ym = max(max(vals+stds*1.96));
        end
        %axis([0 .105 0 ym]);
        subfig = num2str(idata + 3);
        subsubfig = num2str((iclass - 1) * 2 + imet);
        if imet == 1
            a = axis();
            axis([a(1:3), 100]);
        end
        title(strcat('Figure 4.', subfig, '.', subsubfig, ':', CLASS_ALG_STRS{iclass}, METRICS(imet)));
        legend('Bottom-up', 'Bottom-up without HMM', 'Location', 'SouthEast');
        set(gca,'Box','on');

        %print(gcf, '-djpeg', '-r100', strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_', CLASS_ALGS{iclass}, '_cpd_hmm_compare_', lower(METRICS{imet}(2:4)), '_line.jpg'));
        saveas(gcf, strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_', CLASS_ALGS{iclass}, '_hmm_nohmm_compare_', lower(METRICS{imet}(2:4)), '.eps'));
        hold off;
    end
  end
end