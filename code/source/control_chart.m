function[sigma_scores] = control_chart(kpre, data)

assert(mod(kpre,2)==0);
accel = data(:,2:4);
n = size(accel,1);
sigmas = zeros(n-kpre,3);

for i = 1:n-kpre
    xbar_ref = mean(accel(i:i+kpre-1,:));
    s_ref = std(accel(i:i+kpre-1,:),0);
    sigmas(i,:) = (accel(i+kpre,:)-xbar_ref).^2 ./ s_ref.^2;
end

sigma_scores = sqrt(sum(sigmas,2));