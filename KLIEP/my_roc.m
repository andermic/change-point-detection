sc = xbar_score';
assert(size(sc,1) == 1);
true_changes = [190 430 550 621];

thresholds = sort(unique(sc),'descend');

% Each row is a test on all scores for some fixed threshold
pred_changes = (ones(size(thresholds,2),1)*sc) >= (thresholds' * ones(1,size(sc,2)));

detection_times = zeros(size(thresholds,2),1);
false_positives = zeros(size(thresholds,2),1);
for i=1:size(thresholds,2)
    alarms = find(pred_changes(i,:));
    true_positives = [];
    for j=1:(size(true_changes,2)-1)
        for k=alarms
            if (k >= true_changes(j)) && (k < true_changes(j+1))
                true_positives = [true_positives;[k,k-true_changes(j)]];
                break
            end
        end
    end
    detection_times(i) = mean(true_positives(:,2));
    false_positives(i) = size(alarms,2) - size(true_positives,1);
end