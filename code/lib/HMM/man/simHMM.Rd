\encoding{latin1}
\name{simHMM}
\alias{simHMM}
\title{Simulate states and observations for a Hidden Markov Model}
\description{
Simulates a path of states and observations for a given Hidden Markov Model. 
}
\usage{
simHMM(hmm, length)
}
\arguments{
\item{hmm    }{ A Hidden Markov Model.}
\item{length }{ The length of the simulated sequence of observations and states.}
}
\format{
Dimension and Format of the Arguments.
\describe{
	\item{hmm }{A valid Hidden Markov Model, for example instantiated by \code{\link{initHMM}}.}
}
}
\value{
The function \code{simHMM} returns a path of states and associated observations:
\item{states}{ The path of states.}
\item{observations}{ The sequence of observations.}
}
\seealso{
See \code{\link{initHMM}} for instantiation of Hidden Markov Models.
}
\examples{
# Initialise HMM
hmm = initHMM(c("X","Y"),c("a","b","c"))
# Simulate from the HMM
simHMM(hmm, 100)
}
\keyword{models}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}