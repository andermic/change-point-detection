% Generate KLIEP scores for given data.

function [ scores ] = kliep( data, kref, ktest, kernel_width )

addpath('../lib');

n = size(data,1);

% Get LRs for the above dataset using KLIEP.
scores = zeros(n-kref-ktest+1,1);
%tic;
for i=1:n-kref-ktest+1
    d_ref = data(i:i+kref-1);
    d_test = data(i+kref:i+kref+ktest-1);
    if nargin < 4
        temp = KLIEP(d_ref, d_test);
    else
        temp = KLIEP(d_ref, d_test, [], kernel_width);
    end
    scores(i) = temp(kref);
end
%disp('toc:');
%disp(toc);
end
