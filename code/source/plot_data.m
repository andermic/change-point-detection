function [] = plot_data(data_file, cpd_alg, cpd_fpr)

data = csvread(strcat('../../../cpd_data/',data_file));
assert(size(data, 2) == 4);
data = data(:,2:4);

xs = 1:size(data, 1);
plot(xs, data(:,1), xs, data(:,2), xs, data(:,3));

hold on;
mx = max(max(data));
mn = min(min(data));
true_changes = (1:5) * 3600 + 1;
for true_change=true_changes
    plot(repmat(true_change,1,1000), mn:(mx-mn)/999:mx, 'k');
end
strcat('../../results/30hz/predicted_changes_', cpd_alg, '/', cpd_fpr, '/', data_file)
pred_changes = importdata(strcat('../../results/30Hz/predicted_changes_', cpd_alg, '/', cpd_fpr, '/', data_file), ',', 1);
for pred_change=pred_changes.data
    plot(repmat(pred_change,1,100), mn:(mx-mn)/99:mx, 'm');
end
hold off;
pred_changes.data