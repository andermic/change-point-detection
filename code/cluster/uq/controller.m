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

cluster_dir = pwd;
source_dir = '../../source';
lib_dir = '../../lib';
start_at = 1;

for Sim=startchunk:endchunk
     disp(['Simulation ' num2str(Sim) ' of ' num2str(Nsims)])

     % Do some real work here
     addpath(source_dir);
     addpath(lib_dir);

     cc_scores;

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
