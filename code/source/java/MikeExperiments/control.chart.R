controlChart <- function(truncFile, dupFile, seFile, kpre, savePath) {
	se <- read.csv(seFile)
	n <- se[1, 'EndTick']
n=24*30*3600
	data <- data.frame(Axis1=rep(NA, n), Axis2=NA, Axis3=NA)
	sigmas <- data.frame(Axis1=rep(NA, n-kpre), Axis2=NA, Axis3=NA)
		
	print('reading data')
	trunc <- read.csv(truncFile)
	dup <- read.csv(dupFile)
	print('done reading data')
	print('expanding data')
	#data[trunc[, 'Tick'], ] <- trunc[, c('Axis1', 'Axis2', 'Axis3')]
data[trunc[, 'Tick']-21*30*3600, ] <- trunc[, c('Axis1', 'Axis2', 'Axis3')]
	data[which(is.na(data$Axis1)), 'Axis1'] <- rep(dup[, 'Axis1'], dup$Interval)
	data[which(is.na(data$Axis2)), 'Axis2'] <- rep(dup[, 'Axis2'], dup$Interval)
	data[which(is.na(data$Axis3)), 'Axis3'] <- rep(dup[, 'Axis3'], dup$Interval)
	print('done expanding data')

data <- data.frame(Axis1=1:1000, Axis2=2*(1:1000), Axis3=3*(1:1000))
n = 1000

	wind = data[1:kpre, ]
	sum_temp <- colSums(wind)
	sum_sqrd_temp <- colSums(wind^2)
	xbar_ref <- sum_temp / kpre
	s_ref = sqrt((sum_sqrd_temp - (2 * xbar_ref * sum_temp) + xbar_ref^2 * kpre) / (kpre-1))

	print(n)
	print(n-kpre)
	for (i in 1:(n-kpre)) {
		#if ((i %% 100000) == 0) {
			print(i)
		#}
		
		sigmas[i, ] = (data[i+kpre, ] - xbar_ref) ^ 2 / s_ref^2

		sum_temp = sum_temp - wind[1, ]
		sum_sqrd_temp = sum_sqrd_temp - wind[1, ] ^ 2
		wind = rbind(wind[2:kpre, ], data[i+kpre, ])
		sum_temp = sum_temp + wind[kpre, ]
		sum_sqrd_temp = sum_sqrd_temp + wind[kpre, ] ^ 2
		xbar_ref = sum_temp / kpre
		s_ref = sqrt((sum_sqrd_temp - (2 * xbar_ref * sum_temp) + xbar_ref^2 * kpre) / (kpre-1))
		s_ref[s_ref<.Machine$double.eps]
	}
	
	result = sqrt(rowSums(sigmas))
	#write.csv(result) #TODO, finish
	#return(result)
	return(result[1:100,])
}