% Get data from file
data = csvread('1000_labelled_pure.csv');
labels = data(:,1);
accel = data(:,2:4);
n = size(data,1);
line = linspace(0,300,100);

% Plot data
plot_data = data;
plot_data(:,2) = plot_data(:,2) + plot_data(:,3);
plot_data(:,1) = plot_data(:,1) + plot_data(:,2);
figure(1);
plot(1:n,plot_data);
hold on;
plot(240*ones(100),line,'color','k');
hold on;
plot(480*ones(100),line,'color','k');
hold on;
plot(600*ones(100),line,'color','k');
xlabel('Time');
ylabel('Amplitude');

% Plot log LRs for the above dataset using KLIEP
k = 100;
scores = zeros(1,n-k);
for i=1:n-k
    i
    d_ref = accel(i:i+k/2-1);
    d_test = accel(i+k/2:i+k-1);
    temp = KLIEP(d_ref,d_test);
    scores(i) = temp(k/2);
    %scores(i) = sum(log(pre_scores(k/4+1:k/2)./pre_scores(1:k/4)));
end
figure(2);
line = line ./ 5;
plot(k/2+1:n-k/2,scores);
hold on;
plot(240*ones(100),line,'color','g');
hold on;
plot(480*ones(100),line,'color','g');
hold on;
plot(600*ones(100),line,'color','g');