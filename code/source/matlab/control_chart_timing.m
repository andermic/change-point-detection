% Conduct timing experiments for the control chart algorithm

function[time_elapsed] = control_chart_timing(kpre, data, num_ticks, runs_per_tick)

n = size(data,1);

ticks = ceil(rand(1,num_ticks) * (n-kpre-runs_per_tick-1)) + kpre;

tic;
for t = ticks
    sigmas = zeros(runs_per_tick,3);
    wind = data((t-kpre):(t-1),:);
    sum_temp = sum(wind);
    sum_sqrd_temp = sum(wind.^2);
    xbar_ref = sum_temp / kpre;
    s_ref = sqrt((sum_sqrd_temp - (2 .* xbar_ref .* sum_temp) + xbar_ref.^2 * kpre) / (kpre-1));
    s_ref(s_ref<eps) = 1;

    for i = 1:runs_per_tick
        sigmas(i,:) = (data(i+t-1,:)-xbar_ref).^2 ./ s_ref.^2;

        % Compute the sample mean and standard deviation of the reference data along each
        % dimension.
        %xbar_ref = mean(data(i:i+kpre-1,:));
        %s_ref = std(data(i:i+kpre-1,:),0);

        sum_temp = sum_temp - wind(1,:);
        sum_sqrd_temp = sum_sqrd_temp - wind(1,:).^2;
        wind = [wind(2:kpre,:); data(i+t-1,:)];
        sum_temp = sum_temp + wind(kpre,:);
        sum_sqrd_temp = sum_sqrd_temp + wind(kpre,:).^2;
        xbar_ref = sum_temp / kpre;
        s_ref = sqrt((sum_sqrd_temp - (2 .* xbar_ref .* sum_temp) + xbar_ref.^2 * kpre) / (kpre-1));
        s_ref(s_ref<eps) = 1;

        % Compute the number of standard deviations from the test data to the reference
        % data along each dimension.
        %sigmas(i,:) = (data(i+kpre,:)-xbar_ref).^2 ./ s_ref.^2;

    end

    % Return the L2 norm (Mahalanobis distance since we assume the 3 normals are
    % independent) of the sigmas along each dimension.
    %sigma_scores = sqrt(sum(sigmas,2));
end
time_elapsed = toc;
