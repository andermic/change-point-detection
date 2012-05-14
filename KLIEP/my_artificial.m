% Plot artificial dataset 1 from Kawahara & Sugiyama[2009]
n = 1000;
assert(mod(n,100) == 0);
change_at = n/10;
data = zeros(1,n);
mu = 0;
for i=3:n
    data(i) = 0.6*data(i-1) - 0.5*data(i-2) + randn(1) + mu;
    if mod(i,change_at) == 0
        mu = mu + i/change_at;
    end
end
figure(1);
plot(1:n,data);

% Plot log LRs for the above dataset using KLIEP
k = 100;
scores = zeros(1,n-k);
for i=1:n-k
    i
    d_ref = data(i:i+k/2-1);
    d_test = data(i+k/2:i+k-1);
    temp = KLIEP(d_ref,d_test);
    scores(i) = temp(k/2);
    %scores(i) = sum(log(temp(k/4+1:k/2)./temp(1:k/4)));
end
figure(2);
plot(k/2+1:n-k/2,scores);