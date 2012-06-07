function [ srn ] = srn( v )
% Return the sample standard deviation of a column vector over the root of
% the number of elements in the vector

assert(size(v,2)==1);
srn = std(v) / sqrt(size(v,1));

end

