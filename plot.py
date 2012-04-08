from matplotlib.pyplot import *

data = open('1000_V1R3XP_41secDataTable.csv', 'r').readlines()[2:]
data = [[int(j) for j in line.split(',')[2:5]] for line in data]
dataT = [[j[i] for j in data] for i in range(len(data[0]))]
x = range(len(data))
plot(x,dataT[0], x,dataT[1], x,dataT[2])
show()
