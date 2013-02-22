\encoding{latin1}
\name{dishonestCasino}
\alias{dishonestCasino}
\title{Example application for Hidden Markov Models}
\description{
The dishonest casino gives an example for the application of Hidden Markov Models.
This example is taken from Durbin et. al. 1999:
A dishonest casino uses two dice, one of them is fair the other is loaded.
The probabilities of the fair die are (1/6,...,1/6) for throwing ("1",...,"6").
The probabilities of the loaded die are (1/10,...,1/10,1/2) for throwing ("1",..."5","6").
The observer doesn't know which die is actually taken (the state is hidden),
but the sequence of throws (observations) can be used to infer which die (state) was used. 
}
\usage{
dishonestCasino()
}
\format{
The function \code{dishonestCasino} has no arguments.
}
\value{
The function \code{dishonestCasino} returns nothing.
}
\references{
Richard Durbin, Sean R. Eddy, Anders Krogh, Graeme Mitchison (1999). Biological Sequence Analysis:
Probabilistic Models of Proteins and Nucleic Acids. Cambridge University Press. ISBN 0-521-62971-3.
}
\examples{
# Dishonest casino example
dishonestCasino()
}
\keyword{design}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}