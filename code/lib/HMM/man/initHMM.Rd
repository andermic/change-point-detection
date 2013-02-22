\encoding{latin1}
\name{initHMM}
\alias{initHMM}
\title{Initialisation of HMM's}
\description{
This function initialises a general discrete time and discrete space Hidden Markov Model (HMM).
A HMM consists of an alphabet of states and emission symbols. A HMM assumes that
the states are hidden from the observer, while only the emissions of the states are observable.
The HMM is designed to make inference on the states through the observation of emissions.
The stochastics of the  HMM is fully described by the initial starting probabilities of the states,
the transition probabilities between states and the emission probabilities of the states.
}
\usage{
initHMM(States, Symbols, startProbs=NULL, transProbs=NULL, emissionProbs=NULL)
}
\arguments{
\item{States       }{ Vector with the names of the states.}
\item{Symbols      }{ Vector with the names of the symbols.}
\item{startProbs   }{ Vector with the starting probabilities of the states.}
\item{transProbs   }{ Stochastic matrix containing the transition probabilities between the states.}
\item{emissionProbs}{ Stochastic matrix containing the emission probabilities of the states.}
}
\format{
	Dimension and Format of the Arguments.
	\describe{
	\item{States}{     Vector of strings.}
	\item{Symbols}{    Vector of strings.}
	\item{startProbs}{ Vector with the starting probabilities of the states.
	                   The entries must sum to 1.}
	\item{transProbs}{ \code{transProbs} is a (number of states)x(number of states)-sized
	                   matrix, which contains the transition probabilities between states.
	                   The entry \code{transProbs[X,Y]} gives the probability of a transition
	                   from state \code{X} to state \code{Y}. The rows of the matrix must sum to 1.}
	\item{emissionProbs}{ \code{emissionProbs} is a (number of states)x(number of states)-sized
	                   matrix, which contains the emission probabilities of the states.
	                   The entry \code{emissionProbs[X,e]} gives the probability of emission
	                   \code{e} from state \code{X}. The rows of the matrix must sum to 1.}
	}
}
\details{
In \code{transProbs} and \code{emissionProbs} NA's can be used in order to forbid
specific transitions and emissions. This might be useful for Viterbi training or
the Baum-Welch algorithm when using pseudocounts.
}
\value{
The function \code{initHMM} returns a HMM that consists of a list of 5 elements:
\item{States}{ Vector with the names of the states.}
\item{Symbols}{ Vector with the names of the symbols.}
\item{startProbs}{Annotated vector with the starting probabilities of the states.}
\item{transProbs}{ Annotated matrix containing the transition probabilities between the states.}
\item{emissionProbs}{ Annotated matrix containing the emission probabilities of the states.}
}
\references{
For an introduction in the HMM-literature see for example:
\itemize{
\item Lawrence R. Rabiner: A Tutorial on Hidden Markov Models and Selected Applications
in Speech Recognition. Proceedings of the IEEE 77(2) p.257-286, 1989.
\item Olivier Cappe, Eric Moulines, Tobias Ryden: Inference in Hidden Markov Models. Springer. ISBN 0-387-40264-0.
\item Ephraim Y., Merhav N.: Hidden Markov processes. IEEE Trans. Inform. Theory 48 p.1518-1569, 2002.
}}
\seealso{
See \code{\link{simHMM}} to simulate a path of states and observations from a Hidden Markov Model.
}
\examples{
# Initialise HMM nr.1
initHMM(c("X","Y"), c("a","b","c"))
# Initialise HMM nr.2
initHMM(c("X","Y"), c("a","b"), c(.3,.7), matrix(c(.9,.1,.1,.9),2),
        matrix(c(.3,.7,.7,.3),2))
}
\keyword{models}
\author{Lin Himmelmann <hmm@linhi.com>, Scientific Software Development
}