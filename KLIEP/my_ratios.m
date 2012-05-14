% Get data from file
fid = fopen ('Z:\Windows.Documents\Desktop\1000_V1R3XP _ 41secDataTable.csv', 'r');
fgetl (fid);
fgetl (fid);

Y(1,:) = regexp(fgetl (fid), '\,', 'split');

ctr = 1;
while (~feof(fid))
    if ischar (tline)
        ctr = ctr + 1;
        tline = fgetl (fid);
        Y(ctr,:) = regexp(tline, '\,', 'split');
    else
        break;
    end
end
fclose(fid);
Y = (Y[:, 3:5]);
plot(x,Y')