\encoding{latin1}
\name{posterior}
\alias{posterior}
\title{Computes the posterior probabilities for the states}
\description{
This function computes the posterior probabilities of being in state X at time k
for a given sequence of observations and a given Hidden Markov Model.
}
\usage{
posterior(hmm, observation)
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
Return Values:
\item{posterior }{A matrix containing the posterior probabilities.
    The first dimension refers to the state and the second dimension to time.}
}
\references{
Lawrence R. Rabiner: A Tutorial on Hidden Markov Models and Selected Applications
in Speech Recognition. Proceedings of the IEEE 77(2) p.257-286, 1989.
}
\details{
The posterior probability of being in a state X at time k can be computed from the
\code{\link{forward}} and \code{\link{backward}} probabilities:\cr
\code{ Ws(X_k = X | E_1 = e_1, ... , E_n = e_n) = f[X,k] * b[X,k] / Prob(E_1 = e_1, ... , E_n = e_n)}\cr
Where \code{E_1...E_n = e_1...e_n} is the sequence of observed emissions and
\code{X_k} is a random variable that represents the state at time \code{k}.
}
\seealso{
See \code{\link{forward}} for computing the forward probabilities and \code{\link{backward}}
for computing the backward probabilities.
}
\examples{
# Initialise HMM
hmm = initHMM(c("A","B"), c("L","R"), transProbs=matrix(c(.8,.2,.2,.8),2),
	emissionProbs=matrix(c(.6,.4,.4,.6),2))
print(hmm)
# Sequence of observations
observations = c("L","L","R","R")
# Calculate posterior probablities of the states
posterior = posterior(hmm,observations)
print(posterior)
}
\keyword{methods}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}