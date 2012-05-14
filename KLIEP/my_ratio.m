% Assume data is already in an N by 3 matrix
% Plot data
x = data;
n = size(x,1);
x(:,2) = x(:,2) + x(:,3);
x(:,1) = x(:,1) + x(:,2);

figure(1);
plot(1:n, x);

% Plot log LRs for the above dataset using KLIEP
k = 12;
scores = zeros(1,n-k);
for i=1:n-k
    i
    d_ref = x(i:i+k/2-1);
    d_test = x(i+k/2:i+k-1);
    temp = KLIEP(d_ref,d_test,[],1);
    scores(i) = sum(log(temp(k/4+1:k/2)./temp(1:k/4)));
end
figure(2);
plot(k/2+1:n-k+k/2,scores);