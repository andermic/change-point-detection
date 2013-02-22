\encoding{latin1}
\name{baumWelch}
\alias{baumWelch}
\title{Inferring the parameters of a Hidden Markov Model via the Baum-Welch algorithm}
\description{
For an initial Hidden Markov Model (HMM) and a given sequence of observations, the
Baum-Welch algorithm infers optimal parameters to the HMM. Since the Baum-Welch algorithm
is a variant of the Expectation-Maximisation algorithm, the algorithm converges to
a local solution which might not be the global optimum.
}
\usage{
baumWelch(hmm, observation, maxIterations=100, delta=1E-9, pseudoCount=0)
}
\arguments{
\item{hmm           }{ A Hidden Markov Model.}
\item{observation   }{ A sequence of observations.}
\item{maxIterations }{ The maximum number of iterations in the Baum-Welch algorithm.}
\item{delta         }{ Additional termination condition, if the transition
                       and emission matrices converge, before reaching the maximum
                       number of iterations (\code{maxIterations}). The difference
                       of transition and emission parameters in consecutive iterations
                       must be smaller than \code{delta} to terminate the algorithm.}
\item{pseudoCount   }{ Adding this amount of pseudo counts in the estimation-step
                       of the Baum-Welch algorithm.}
}
\format{
Dimension and Format of the Arguments.
\describe{
	\item{hmm         }{A valid Hidden Markov Model, for example instantiated by \code{\link{initHMM}}.}
	\item{observation }{A vector of observations.}
}
}
\value{
Return Values:
\item{hmm        }{The inferred HMM. The representation is equivalent to the
                   representation in \code{\link{initHMM}}.}
\item{difference }{Vector of differences calculated from consecutive transition and emission
                   matrices in each iteration of the Baum-Welch procedure.
                   The difference is the sum of the distances between consecutive
                   transition and emission matrices in the L2-Norm.}
}
\references{
For details see: Lawrence R. Rabiner: A Tutorial on Hidden Markov Models and Selected Applications
in Speech Recognition. Proceedings of the IEEE 77(2) p.257-286, 1989.
}
\examples{
# Initial HMM
hmm = initHMM(c("A","B"),c("L","R"),
	transProbs=matrix(c(.9,.1,.1,.9),2),
	emissionProbs=matrix(c(.5,.51,.5,.49),2))
print(hmm)
# Sequence of observation
a = sample(c(rep("L",100),rep("R",300)))
b = sample(c(rep("L",300),rep("R",100)))
observation = c(a,b)
# Baum-Welch
bw = baumWelch(hmm,observation,10)
print(bw$hmm)
}
\seealso{
See \code{\link{viterbiTraining}}.
}
\keyword{methods}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}