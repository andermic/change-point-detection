% Helper function for cc_scores

function [data] = read_data(data_file)

data = csvread(strcat('../../../cpd_data/',data_file));
assert(size(data, 2) == 4);

end
