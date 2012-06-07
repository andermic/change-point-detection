function [false_positive_rates, accuracy_rates, detection_times  ] = amot_roc( scores, true_changes )
% Generate false positives, accuracy rates, and detection times so that 
% they may be plotted for both amot and roc curves.

sc = scores';
assert(size(sc,1) == 1);

% .99999 factor is to correct a numerical stability issue
thresholds = logspace(log(min(sc)+eps)/log(10),log(max(sc)*.99999)/log(10),200);
thresholds = sort(thresholds,'descend');

% Each row is a test on all scores for some fixed threshold
pred_changes = (ones(size(thresholds,2),1)*sc) >= (thresholds' * ones(1,size(sc,2)));

detection_times = zeros(size(thresholds,2),1);
false_positive_rates = zeros(size(thresholds,2),1);
accuracy_rates = zeros(size(thresholds,2),1);
true_changes = [1;true_changes;size(sc,2)];
for i=1:size(thresholds,2)
    alarms = find(pred_changes(i,:));
    true_positives = [];
    for j=1:(size(true_changes,1)-1)
        for k=alarms
            if (k >= true_changes(j)) && (k < true_changes(j+1))
                true_positives = [true_positives;[k,k-true_changes(j)]];
                break
            end
        end
    end
    detection_times(i) = mean(true_positives(:,2));
    false_positive_rates(i) = (size(alarms,2) - size(true_positives,1))/size(sc,2);
    accuracy_rates(i) = size(true_positives,1) / (size(true_changes,1) - 1);
end

end
