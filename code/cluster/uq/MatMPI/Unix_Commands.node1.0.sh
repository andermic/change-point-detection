#!/bin/sh
#$ -S /bin/sh
#$ -N andermic_job
#$ -cwd
#$ -j y
#$ -m e
#$ -M andermic@engr.oregonstate.edu
#$ -q eecs1

touch MatMPI/pid.node1.$!
/nfs/stak/a1/rhel5apps/matlab2011b/bin/matlab -display null -nojvm -nosplash  < MatMPI/MatMPIdefs0.m > MatMPI/controller.0.out
