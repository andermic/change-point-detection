\encoding{latin1}
\name{backward}
\alias{backward}
\title{Computes the backward probabilities}
\description{
The \code{backward}-function computes the backward probabilities.
The backward probability for state X and observation at time k is defined as the probability
of observing the sequence of observations e_k+1, ... ,e_n under the condition that the
state at time k is X. That is:\cr
\code{b[X,k] := Prob(E_k+1 = e_k+1, ... , E_n = e_n | X_k = X)}.\cr
Where \code{E_1...E_n = e_1...e_n} is the sequence of observed emissions and
\code{X_k} is a random variable that represents the state at time \code{k}.
}
\usage{
backward(hmm, observation)
}
\arguments{
\item{hmm         }{ A Hidden Markov Model.}
\item{observation }{ A sequence of observations.}
}
\format{
Dimension and Format of the Arguments.
\describe{
	\item{hmm         }{A valid Hidden Markov Model, for example instantiated by \code{\link{initHMM}}.}
	\item{observation }{A vector of strings with the observations.}
}
}
\value{
Return Value:
\item{backward }{A matrix containing the backward probabilities.
  The probabilities are given on a logarithmic scale (natural logarithm).
  The first dimension refers to the state and the second dimension to time.}
}
\seealso{
See \code{\link{forward}} for computing the forward probabilities.
}
\references{
Lawrence R. Rabiner: A Tutorial on Hidden Markov Models and Selected Applications
in Speech Recognition. Proceedings of the IEEE 77(2) p.257-286, 1989.
}
\examples{
# Initialise HMM
hmm = initHMM(c("A","B"), c("L","R"), transProbs=matrix(c(.8,.2,.2,.8),2),
	emissionProbs=matrix(c(.6,.4,.4,.6),2))
print(hmm)
# Sequence of observations
observations = c("L","L","R","R")
# Calculate backward probablities
logBackwardProbabilities = backward(hmm,observations)
print(exp(logBackwardProbabilities))
}
\keyword{methods}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}