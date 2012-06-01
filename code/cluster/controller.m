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
Nsims = 91;

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
source_dir = '../source';
lib_dir = '../lib';
data_dir = '../../../cpd_data';
start_at = 1;

cd ../../../cpd_data;
data_instances = ['1000_V1.csv';'1000_V2.csv';'1005_V1.csv';'1005_V2.csv';'1006_V1.csv';'1008_V1.csv';'1008_V2.csv';'1009_V1.csv';'1009_V2.csv';'1010_V1.csv';'1010_V2.csv';'1011_V1.csv';'1021_V2.csv';'1024_V1.csv';'1024_V2.csv';'1025_V1.csv';'1025_V2.csv';'1028_V1.csv';'1028_V2.csv';'1029_V1.csv';'1029_V2.csv';'1032_V1.csv';'1033_V1.csv';'1033_V2.csv';'1034_V1.csv';'1034_V2.csv';'1035_V1.csv';'1035_V2.csv';'1040_V1.csv';'1040_V2.csv';'1041_V1.csv';'1041_V2.csv';'1042_V1.csv';'1042_V2.csv';'1049_V1.csv';'1049_V2.csv';'1052_V1.csv';'1052_V2.csv';'1053_V1.csv';'1053_V2.csv';'1054_V1.csv';'1054_V2.csv';'1056_V1.csv';'1056_V2.csv';'1065_V1.csv';'1065_V2.csv';'1066_V1.csv';'1068_V1.csv';'1068_V2.csv';'1069_V1.csv';'1069_V2.csv';'1070_V1.csv';'1070_V2.csv';'1071_V2.csv';'1072_V1.csv';'1072_V2.csv';'1078_V1.csv';'1078_V2.csv';'1084_V1.csv';'1084_V2.csv';'1087_V1.csv';'1087_V2.csv';'1088_V1.csv';'1093_V1.csv';'1093_V2.csv';'1094_V1.csv';'1094_V2.csv';'1101_V1.csv';'1101_V2.csv';'1103_V1.csv';'1103_V2.csv';'1104_V1.csv';'1104_V2.csv';'1115_V1.csv';'1115_V2.csv';'1117_V1.csv';'1117_V2.csv';'1118_V1.csv';'1118_V2.csv';'1119_V1.csv';'1119_V2.csv';'1128_V1.csv';'1128_V2.csv';'1130_V1.csv';'1130_V2.csv';'1131_V2.csv';'1151_V2.csv';'1155_V1.csv';'1155_V2.csv';'2118_V1.csv';'2118_V2.csv'];
cd ../change_point_detection/code/cluster;
secs_pre = 10;
secs_post = 1;

for Sim=startchunk:endchunk
     disp(['Simulation ' num2str(Sim) ' of ' num2str(Nsims)])

     % Do some real work here
     addpath(source_dir);
     addpath(lib_dir);
     addpath(data_dir);

     this_instance = data_instances(Sim+start_at-1,:); 
     data = csvread(this_instance);
     disp(kliep(data, secs_pre*30, secs_post*30));
     disp(this_instance);

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
