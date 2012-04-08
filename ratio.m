(* ::Package:: *)

% Initialize Y
% {
% i) Read Y from a csv data file
fid = fopen ('Z:\Windows.Documents\Desktop\1000_V1R3XP _ 41secDataTable.csv', 'r');
fgetl (fid);
fgetl (fid);

Y (1,:) = regexp (fgetl (fid), '\,', 'split');

ctr = 1;
while (~feof(fid))
    if ischar (tline)
        ctr = ctr + 1;
        tline = fgetl (fid);
        Y (ctr,:) = regexp (tline, '\,', 'split');
    else
        break;
    end
end
fclose (fid);
Y = (data[:, 3:5]);
plot (x,Y')
%}

% ii) Create synthetic Y to test algorithm
% iia) Use concatenated sequences of normally distributed data with
% different means.
MEAN_1 = 0;
MEAN_ 2 = 10;
Y = vertcat (randn (1000,1)+MEAN_ 1, randn (1000,1)+MEAN_ 2);

% 1
sigma = 1;
K = ;
b = ;

% 2
 
  


% 3
a = ones ();
e = 0.01;

% 4
adjust = e.*K'./(K.*a)
while adjust ~= 0
	% 5
	a = a + adjust;
	
	% 6
	a = a + (1-b'.*a)*b/(b'*b);
	a = max (0,a);
	a = a/(b'*a)
% 7
end

