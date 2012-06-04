function [] = my_plot(k, data, scores, true_change_points)
% UNDER CONSTRUCTION
% Plot data and change point detection scores as a function of time. Plots
% should include true change points.

% Plot data
plot_data = data;
plot_data(:,2) = plot_data(:,2) + plot_data(:,3);
plot_data(:,1) = plot_data(:,1) + plot_data(:,2);
line = linspace(0,max(max(plot_data))*5/4,100);

% FIX: should plot arbitrary change points defined by function argument.
figure(1);
plot(1:n,plot_data);
hold on;
plot(120*ones(100),line,'color','g');
hold on;
plot(240*ones(100),line,'color','g');
hold on;
plot(360*ones(100),line,'color','g');
hold on;
plot(480*ones(100),line,'color','g');
hold on;
plot(600*ones(100),line,'color','g');
xlabel('Time');
ylabel('Amplitude');

figure(2);
line = line ./ 5;
plot(k/2+1:n-k/2,scores);

% FIX: should plot arbitrary change points defined by function argument.
hold on;
plot(120*ones(100),line,'color','g');
hold on;
plot(240*ones(100),line,'color','g');
hold on;
plot(360*ones(100),line,'color','g');
hold on;
plot(480*ones(100),line,'color','g');
hold on;
plot(600*ones(100),line,'color','g');

end

