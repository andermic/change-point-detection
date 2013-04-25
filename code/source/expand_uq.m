function [ data ] = expand_uq( trunc_file, dup_file, se_file)

%expand_uq      - Expand a condensed representation of a UQ .csv file
%   in:
%   truncFile   - Path to the truncated file
%   dupFile     - Path to the duplicates file
%   se_file     - Path to the start_and_end_file
%
%   out:
%   data        - The expanded data

se = csvread(se_file,1,4);
end_tick = se(1)
data = repmat(Inf, end_tick, 3);

disp('reading data');
trunc = csvread(trunc_file, 1, 0);
disp('done reading data');
data(trunc(:,1), :) = trunc(:, 2:4);

dup = csvread(dup_file, 1, 0);
h = [];
h(cumsum(dup(:,5))) = 1;
data(find(data(:,1)==Inf), :) = dup(cumsum(h)-h+1, 2:4);

end