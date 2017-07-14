0001 - Streaming Writes
-----------------------

Context
^^^^^^^

To write streaming data (e.g. ``RDD[(K, V)]``) to an ``S3`` backend it
is necessary to map over rdd partitions and to send multiple async PUT
requests for all elements of a certain partition, it is important to
synchronize these requests in order to be sure, that after calling a
``writer`` function all data was ingested (or at least attempted). Http
status error ``503 Service Unavailable`` requires resending a certain
PUT request (with exponential backoff) due to possible network problems
this error was caused by. ``Accumulo`` and ``Cassandra`` writers work in
a similar fashion.

To handle this situation we use the ``Task`` abstraction from Scalaz,
which uses it's own ``Future`` implementation. The purpose of this
research is to determine the possibility of removing the heavy
``Scalaz`` dependency. In a near future we will likely depend on the
``Cats`` library, which is lighter, more modular, and covers much of the
same ground as Scalaz. Thus, to depend on ``Scalaz`` is not ideal.

Decision
^^^^^^^^

We started by a moving from Scalaz ``Task`` to an implementation based
on the scala standard library ``Future`` abstraction. Because
``List[Future[A]]`` is convertable to ``Future[List[A]]`` it was thought
that this simpler home-grown solution might be a workable alternative.

Every ``Future`` is basically some calculation that needs to be
submitted to a thread pool. When you call
``(fA: Future[A]).flatMap(a => fB: Future[B])``, both ``Future[A]`` and
``Future[B]`` need to be submitted to the thread pool, even though they
are not running concurrently and could run on the same thread. If
``Future`` was unsuccessful it is possible to define recovery strategy
(in case of ``S3`` it is neccesary).

We faced two problems: difficulties in ``Future`` synchronization
(``Future.await``) and in ``Future`` delay functionality (as we want an
exponential backoff in the ``S3`` backend case).

We can await a ``Future`` until it's done (``Duration.Inf``), but we can
not be sure that ``Future`` was completed exactly at this point (for
some reason - this needs further investigation - it completes a bit
earlier/later).

Having a threadpool of ``Future``\ s and having some ``List[Future[A]``,
awaiting of these ``Futures`` does not guarantees completeness of each
``Future`` of a threadpool. Recovering a ``Future`` we produce a *new*
``Future``, so that recoved ``Future``\ s and recursive ``Future``\ s
are *new* ``Future``\ s in the same threadpool. It isn't obvious how to
await all *necessary* ``Future``\ s. Another problem is *delayed*
``Future``\ s, in fact such behaviour can only be achieved by creating
*blocking* ``Future``\ s. As a workaround to such a situation, and to
avoid *blocking* ``Future``\ s, it is possible to use a ``Timer``, but
in fact that would be a sort of separate ``Future`` pool.

Let's observe Scalaz ``Task`` more closely, and compare it to native
scala ``Future``\ s. With ``Task`` we recieve a bit more control over
calculations. In fact ``Task`` is not a concurrently running
computation, it’s a description of a computation, a lazy sequence of
instructions that may or may not include instructions to submit some of
calculations to thread pools. When you call
``(tA: Task[A]).flatMap(a => tB: Task[B])``, the ``Task[B]`` will by
default just continue running on the same thread that was already
executing ``Task[A]``. Calling ``Task.fork`` pushes the task into the
thread pool. Scalaz ``Task``\ s operates with their own ``Future``
implementation. Thus, having a stream of ``Task``\ s provides more
control over concurrent computations.

`Some
implementations <https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b>`__
were written, but each had synchronization problems. This attempt to get
rid of the Scalaz dependency is not as trival as we had anticipated.

This is not a critical decision and, if necessary, we can come back to
it later.

Consequences
^^^^^^^^^^^^

All implementations based on ``Future``\ s are non-trival, and it
requires time to implement a correct write stream based on native
``Future``\ s.
`Here <https://gist.github.com/pomadchin/33b53086cbf81a6256ddb452090e4e3b>`__
are the two simplest and most transparent implementation variants, but
both have synchronization problems.

Scalaz ``Task``\ s seem to be better suited to our needs. ``Task``\ s
run on demand, and there is no requirement of instant submission of
``Task``\ s into a thread pool. As described above, ``Task`` is a lazy
sequence of intructions and some of them could submit calculations into
a thread pool. Currently it makes sense to depend on Scalaz.
