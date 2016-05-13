0001 - Streaming Writes
===============================

Context
-------
To write streaming data (in particular case to write `RDD[(K, V)]`) to S3 backend it is necessary to map over rdd partitions and to send multiple async put requests for all elements of a certain partition, it is important to synchronize these requests in order to be sure, that after calling writer function all data was ingested (at least tried and the result in known). Http status error `503 Service Unavailable` requires resending a certain put request (with exponential backoff), due to a possible network problems this error was caused by. In a similar way works Accumulo and Cassandra writers.

To handle that situation we are use Scalaz Task abstraction, which uses it's own Futures implementation. The purpose of this research is to get rid of Scalaz dependency as in a near future we can depend on Cats library, and to have Scalaz in dependencies in this case is not reasonable. 

Decision
--------
We started by a moving from Scalaz Tasks to an implementation based on scala native Futures abstraction. List of Futures is convertable to Future of List, that was one of motivations to try this approach as an alternative to Scalaz Task, as it would be possible to await _sequence_ of Futures (means to await all of them to be finished). 

Every Future is basically some work, that needs to be submitted to a thread pool. When you call (fA: Future[A]).flatMap(a => fB: Future[B]), both Future[A] and Future[B] need to be submitted to the thread pool, even though they are not running concurrently and could run on the same thread. If Future was unsuccessful it is possible to define recovery strategy (in case of S3 it is neccesary). 

We faced two basal problems: difficulties in Futures synchronization (Future await) and in Future delay function (as we want an exponential backoff in S3 backend case). 

We can Await Future until it's done (Duration.Inf), but we can not be sure that Future was completed exactly at this point (for some reason, that needs further investigations, it completes a bit after / earlier). 

Having a threadpool of futures and having some list of futures, awaiting of these futures obviously not guarantees completeness of each future of a threadpool. Recovering a Future, in fact we produce a _new_ Future, so recoved Futures and recursive Futures are _new_ Futures in the same threadpool, and that's not obvious how to await all _necessary_ Futures. Another problem is _delayed_ Futures, in fact this could only be achieved by creating _blocking_ Futures. As a workaround to such situation and not to produce _blocking_ Futures is to use timer, but in fact that would be a sort of separate Future pool.

Let's observe Scalaz Task more close, and compare it to native scala Futures.
With Task we recieve a bit more control over calculations. In fact Task is not a concurrently running computation, it’s a description of a computation, a sequence of instructions that may include instructions to submit some of calculations to thread pools. When you call (tA: Task[A]).flatMap(a => tB: Task[B]), the Task[B] will by default just continue running on the same thread that was already executing Task[A]. Calling Task.fork pushes task into the thread pool. Scalaz Tasks operates with it's own Future implementation. Having a stream of Tasks provides us more control over concurrent computations. 

[Some implementations](https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b) were wrote, but each had synchronization problems, this attempt to get rid of scalaz dependency is not as trival as we thought. 

Right now that's not a critical decision and if that would be necessary we can come back to it later.

Consequences
------------
All implementations based on Futures are not trival, and it requires more time to implement correct write stream based on native Futures. [Here](https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b) provided two easiest and transparent implementations variants.

Lastly, Scalaz Tasks seems to be more optimal at least because of the reason, that all tasks run on demand, and there is no requirement in instant submition of Tasks into a thread pool, as already been described above Task is a sequence of intructions and some of them could submit calculations into a thread pool.
