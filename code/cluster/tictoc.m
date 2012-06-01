function[res] = tictoc(n)

A = rand(n,n);
b = rand(n,1);
tic;
x = A\b;
res = toc;

