function [ scores ] = kliep( data, kref, ktest )
% Generate KLIEP LR scores for given data.

labels = data(:,1);
accel = data(:,2:4);
n = size(data,1);

% Get LRs for the above dataset using KLIEP.
scores = zeros(n-kref-ktest+1,1);
tic;
for i=1:n-kref-ktest+1
    i
    d_ref = accel(i:i+kref-1);
    d_test = accel(i+kref:i+kref+ktest-1);
    temp = KLIEP(d_ref,d_test);
    scores(i) = temp(kref);
end
'toc:'
toc
end