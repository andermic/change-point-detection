% Delete left over MPI directory
MatMPI_Delete_all;
pause(2.0);

% Number of nodes to use on the cluster
num_nodes = 1;

% Name of matlab file to run on the cluster
% Change this line to be the name of your .m file
m_name = 'controller';

% Define machine names to run on cluster
machines = cell(1,num_nodes);

for node=1:num_nodes
     machines{node} = strcat('node',num2str(node));
end

% call the MPI commands to run the code on the cluster
eval ( MPI_Run(m_name, num_nodes, machines ) );
