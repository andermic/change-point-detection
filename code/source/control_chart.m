function[sigma_scores] = control_chart(kpre, data)

accel = data(:,2:4);
n = size(accel,1);
sigmas = zeros(n-kpre,3);

for i = 1:n-kpre
	% Compute the sample mean and standard deviation of the reference data along each
	% dimension.
    xbar_ref = mean(accel(i:i+kpre-1,:));
    s_ref = std(accel(i:i+kpre-1,:),0);

	% Compute the number of standard deviations from the test data to the reference
	% data along each dimension.
    sigmas(i,:) = (accel(i+kpre,:)-xbar_ref).^2 ./ s_ref.^2;
end

% Return the L2 norm (Mahalanobis distance since we assume the 3 normals are
% independent) of the sigmas along each dimension.
sigma_scores = sqrt(sum(sigmas,2));
