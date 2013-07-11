% Conduct KLIEP timing experiments.

function [time_elapsed] = kliep_timing(num_runs)

KREF = 300;
KTEST = 30;

addpath('../../../cpd_data');
data = csvread('1000_V1R3XP_4.csv');
data = data(:,2:4);
n = size(data,1);
ticks = ceil(rand(1,num_runs) * (n-KREF-KTEST-1)) + 1;
tic;
for t = ticks
    t;
    kliep(data(t:(t+KREF+KTEST-1),:), KREF, KTEST,1);
end
time_elapsed = toc;

