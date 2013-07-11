% Calculate control chart scores for each tick in the time series.

function[sigma_scores] = control_chart(kpre, data)

n = size(data,1);
sigmas = zeros(n-kpre,3);

wind = data(1:kpre,:);
sum_temp = sum(wind);
sum_sqrd_temp = sum(wind.^2);
xbar_ref = sum_temp / kpre;
s_ref = sqrt((sum_sqrd_temp - (2 .* xbar_ref .* sum_temp) + xbar_ref.^2 * kpre) / (kpre-1));
s_ref(s_ref<eps) = 1;

tic;
for i = 1:n-kpre
    if (mod(i, 100000) == 0)
        disp(i);
    end
    
    sigmas(i,:) = (data(i+kpre,:)-xbar_ref).^2 ./ s_ref.^2;
    
	% Compute the sample mean and standard deviation of the reference data along each
	% dimension.
    %xbar_ref = mean(data(i:i+kpre-1,:));
    %s_ref = std(data(i:i+kpre-1,:),0);
    
    sum_temp = sum_temp - wind(1,:);
    sum_sqrd_temp = sum_sqrd_temp - wind(1,:).^2;
    wind = [wind(2:kpre,:); data(i+kpre,:)];
    sum_temp = sum_temp + wind(kpre,:);
    sum_sqrd_temp = sum_sqrd_temp + wind(kpre,:).^2;
    xbar_ref = sum_temp / kpre;
    s_ref = sqrt((sum_sqrd_temp - (2 .* xbar_ref .* sum_temp) + xbar_ref.^2 * kpre) / (kpre-1));
    s_ref(s_ref<eps) = 1;

    
	% Compute the number of standard deviations from the test data to the reference
	% data along each dimension.
    %sigmas(i,:) = (data(i+kpre,:)-xbar_ref).^2 ./ s_ref.^2;
    
end
disp('toc:');
toc

% Return the L2 norm (Mahalanobis distance since we assume the 3 normals are
% independent) of the sigmas along each dimension.
sigma_scores = sqrt(sum(sigmas,2));
