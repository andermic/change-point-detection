% Helper function for cc_aggregate
% Find the true changes in a given data instance

function [ res ] = changes( data )

res = [];
for i = 1:(size(data,1)-1)
    if data(i,1) ~= data(i+1,1)
        res = [res;i+1];
    end
end

