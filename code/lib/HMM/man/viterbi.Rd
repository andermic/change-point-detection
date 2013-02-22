\encoding{latin1}
\name{viterbi}
\alias{viterbi}
\title{Computes the most probable path of states}
\description{
The Viterbi-algorithm computes the most probable path of states for a sequence
of observations for a given Hidden Markov Model.
}
\usage{
viterbi(hmm, observation)
}
\arguments{
\item{hmm         }{ A Hidden Markov Model.}
\item{observation }{ A sequence of observations.}
}
\format{
Dimension and Format of the Arguments.
\describe{
	\item{hmm         }{A valid Hidden Markov Model, for example instantiated by \code{\link{initHMM}}.}
	\item{observation }{A vector of observations.}
}
}
\value{
Return Value:
\item{viterbiPath }{A vector of strings, containing the most probable path of states.}
}
\references{
Lawrence R. Rabiner: A Tutorial on Hidden Markov Models and Selected Applications
in Speech Recognition. Proceedings of the IEEE 77(2) p.257-286, 1989.
}
\examples{
# Initialise HMM
hmm = initHMM(c("A","B"), c("L","R"), transProbs=matrix(c(.6,.4,.4,.6),2),
	emissionProbs=matrix(c(.6,.4,.4,.6),2))
print(hmm)
# Sequence of observations
observations = c("L","L","R","R")
# Calculate Viterbi path
viterbi = viterbi(hmm,observations)
print(viterbi)
}
\keyword{methods}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}