k = 100; 
assert(mod(k,2)==0);
n = size(accel,1);
sigma_score = [];

for i = 1:n-k/2
    xbar_ref = mean(accel(i:i+k/2-1,:));
    s_ref = std(accel(i:i+k/2-1,:),0);
    sigma_score = [sigma_score;abs(accel(i+50,:)-xbar_ref) ./ s_ref];
end
sigma_score = mean(sigma_score,2);