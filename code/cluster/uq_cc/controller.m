avail=license('checkout','MATLAB'); 
while avail == 0
     disp('License not available, waiting...');
     pause(10.0);
     avail=license('checkout','MATLAB');
end
disp('Got the needed license'); 

% Initialize MPI environment
MPI_Init;
comm = MPI_COMM_WORLD;
comm_size = MPI_Comm_size(comm);
my_rank = MPI_Comm_rank(comm);

disp(['my_rank: ',num2str(my_rank)]);
disp(['comm size: ',num2str(comm_size)]);
leader = 0;

% Load data file. This is used if data is created before simulations are to be run.
%load('SimData.mat');

% Number of simulations
Nsims = 1;

% Size of array
%[n Nsims]=size(YsimOS); % YsimOS is the name of the data matrix read in the above load()
                        % statement. Nsims just needs to be the number of simulations
                        % to run.

% Break the job into pieces
chunksize = floor(Nsims / comm_size)
startchunk = chunksize * (my_rank) + 1
endchunk = startchunk + chunksize - 1
if (Nsims - endchunk > 0 & Nsims - endchunk < chunksize)
     endchunk = Nsims
end

% To prevent file locking problems, output to separate files
filename = strcat('SimResults.',num2str(my_rank));

source_dir = '../../source';
lib_dir = '../../lib';
start_at = 1;

kpre = 300;
folders = [1:4 6:16 17:25]
trunc_files = arrayfun(@(x) strcat('/nfs/guille/wong/users/andermic/uq/processed/', num2str(x), '/', num2str(x), '_30hz_truncated.csv'), folders, 'UniformOutput', false)
dup_files = arrayfun(@(x) strcat('/nfs/guille/wong/users/andermic/uq/processed/', num2str(x), '/', num2str(x), '_30hz_duplicates.csv'), folders, 'UniformOutput', false)
se_files = arrayfun(@(x) strcat('/nfs/guille/wong/users/andermic/uq/processed/', num2str(x), '/', num2str(x), '_30hz_start_and_end.csv'), folders, 'UniformOutput', false)
out_files = arrayfun(@(x) strcat('/nfs/guille/wong/users/andermic/uq/changepoints/cc_kpre', num2str(kpre), '/scores_', num2str(x), '.csv', folders, 'UniformOutput', false)

for Sim=startchunk:endchunk
     disp(['Simulation ' num2str(Sim) ' of ' num2str(Nsims)])

     % Do some real work here
     addpath(source_dir);
     addpath(lib_dir);
     addpath(data_dir);

     trunc_file = trunc_files(Sim+start_at-1, :);
     dup_file = dup_files(Sim+start_at-1, :);
     se_file = se_files(Sim+start_at-1, :);
     out_file = out_files(sim+start_at-1, :);
     disp('expanding data')
     data = expand_uq(trunc_file, dup_file, se_file);

data = data[626640:627640,:]

     disp('done expanding')
     csvwrite(out_file, control_chart(kpre, data));

     fid=fopen(filename,'a'); % Open the output file to write results for current iteration.
     fprintf(fid,'%d\t',Sim);
     fprintf(fid,'\n');
     fclose(fid);
end

% Cleanup MPI environment
MPI_Finalize;
disp('SUCCESS');
if (my_rank ~= MatMPI_Host_rank(comm))
     exit;
end
