k = 100; 
assert(mod(k,2)==0);
n = size(accel,1);
xbar_score = [];

for i = 1:n-k+1
    xbar_ref = mean(accel(i:i+k/2-1,:));
    xbar_test = mean(accel(i+k/2:i+k-1,:));
    xbar_score = [xbar_score;abs(xbar_test-xbar_ref)];
end
xbar_score = mean(xbar_score,2);