ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/results/30hz';
ROOT_OUTPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/thesis/figures';
DATASETS = {{'OSU_YR4_Hip_30Hz.ws120.7cls','uq_30Hz_day2','uq_30Hz_day3'},{'OSU_YR4_Hip_30Hz.HMM.7cls','uq_30Hz_day2.HMM','uq_30Hz_day3.HMM'}};
DATASET_STRS = {' OSU Hip', ' LiME Day 1', ' LiME Day 2'};
DATASET_FILE_STRS = {'osu', 'lime1', 'lime2'};
CLASS_ALGS = {'dt', 'svm', 'nnet'};
CLASS_ALG_STRS = {' Decision Tree', ' SVM', ' Neural Network'};
CPD_ALGS = {'cc', 'kliep'};
METRICS = {' Accuracy', ' Detection Time'};
FPRS = [0.0005 0.001 0.005 0.01];
FPRS_STR = {'0.005' '0.01' '0.05' '0.1'};
WSES = 10:2:20;
WSES_STR = {'10' '12' '14' '16' '18' '20'};

REF_WINDOW_SIZE = 1;

addpath(ROOT_INPUT_FOLDER);
for idata = 1:3
    for ialg = 1:3
        for imet = 1:2
            input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', CLASS_ALGS{ialg}, '_cc_results.csv');
            cc = csvread(input_file, 1);
            input_file = strcat(ROOT_INPUT_FOLDER, '/cpd/', DATASETS{1}{idata}, '/', CLASS_ALGS{ialg}, '_kliep_results.csv');
            kliep = csvread(input_file, 1);
            kliep(:,4) = kliep(:,4) + REF_WINDOW_SIZE;
            cc_indices = [];
            kliep_indices = [];
            for fpr = FPRS
                cc_indices = [cc_indices, find(cc(:,1)==fpr)];
                kliep_indices = [kliep_indices, find(kliep(:,1)==fpr)];
            end
            cc = cc(cc_indices, :);
            kliep = kliep(kliep_indices, :);
            if imet == 1
                cc(:,2:3) = cc(:,2:3) * 100;
                kliep(:,2:3) = kliep(:,2:3) * 100;
            end
            vals = [cc(:,imet*2), kliep(:,imet*2)];
            stds = [cc(:,imet*2+1), kliep(:,imet*2+1)];
            f = bar(vals);
            set(f(:,1), 'facecolor', [0.8 0.8 0.8]);
            set(f(:,2), 'facecolor', [1 1 1]);
            hold on;
            errorbar([(1:size(FPRS,2))'-.15, (1:size(FPRS,2))'+.15], vals, stds*1.96/sqrt(30), 'black', 'linestyle', 'none')
            set(gca, 'XTick', 1:size(FPRS,2), 'XTickLabel', FPRS_STR);
            xlabel('False Positives / s');
            if imet == 1
                ylabel(strcat(METRICS{imet}, ' (%)'));
                ym = 100;
            else
                ylabel(strcat(METRICS{imet}, ' (s)'));
                ym = max(max(vals+stds*1.96/sqrt(30)))*1.1;
            end
            figsub = num2str(idata);
            figsubsub = num2str((ialg - 1)*2 + imet);
            title(strcat('Figure 4.', figsub, '.', figsubsub, ': ', CLASS_ALG_STRS{ialg}, METRICS{imet}));
            axis([.5 size(FPRS,2)+.5 0 ym]);
            legend('CC', 'KLIEP');
            saveas(gcf, strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_cpd_', CLASS_ALGS{ialg}, '_', lower(METRICS{imet}(2:4)), '.eps'));
            hold off;
        end
    end
end

for idata = 1:3
    for ialg = 1:3
        for imet = 1:2
            input_file = strcat(ROOT_INPUT_FOLDER, '/hmm/', DATASETS{2}{idata}, '/', CLASS_ALGS{ialg}, '_results.csv');
            results = csvread(input_file, 1);
            indices = [];
            for ws = WSES
                indices = [indices, find(results(:,1)==ws)];
            end
            results = results(indices, :);
            if imet == 1
                results(:,2:3) = results(:,2:3) * 100;
            end
            vals = results(:,imet*2);
            stds = results(:,imet*2+1);
            f = bar(vals);
            set(f(:,1), 'facecolor', [0.8 0.8 0.8]);
            hold on;
            lower_bars = stds*1.96/sqrt(10);
            lower_bars(lower_bars > vals) = vals(lower_bars > vals) *.99;
            errorbar(1:size(WSES,2), vals, lower_bars, stds*1.96/sqrt(10), 'black', 'linestyle', 'none');
            set(gca, 'XTick', 1:size(WSES,2), 'XTickLabel', WSES_STR);
            xlabel('Window Sizes (s)');
            if imet == 1
                ylabel(strcat(METRICS{imet}, ' (%)'));
                ym = 100;
            else
                ylabel(strcat(METRICS{imet}, ' (s)'));
                ym = max(max(vals+stds*1.96/sqrt(10)))*1.1;
            end
            figsub = num2str(idata + 3);
            figsubsub = num2str((ialg - 1)*2 + imet);
            title(strcat('Figure 4.', figsub, '.', figsubsub, ': ', CLASS_ALG_STRS{ialg}, METRICS{imet}));
            %ymax = round(max(max(vals+stds*1.96/sqrt(10)))*1.2);
            %if imet == 1 && ymax < 100
            %    ymax = 100;
            %end
            a = axis();
            a(4) = ym;
            axis(a);
            saveas(f, strcat(ROOT_OUTPUT_FOLDER, '/', DATASET_FILE_STRS{idata}, '_hmm_', CLASS_ALGS{ialg}, '_', lower(METRICS{imet}(2:4)), '.eps'));
            hold off;
        end
    end
end