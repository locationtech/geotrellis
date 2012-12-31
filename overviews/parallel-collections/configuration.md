---
layout: overview-large
title: Configuring Parallel Collections

disqus: true

partof: parallel-collections
num: 7
---

## Task support

Parallel collections are modular in the way operations are scheduled. Each
parallel collection is parametrized with a task support object which is
responsible for scheduling and load-balancing tasks to processors.

The task support object internally keeps a reference to a thread pool
implementation and decides how and when tasks are split into smaller tasks. To
learn more about the internals of how exactly this is done, see the tech
report \[[1][1]\].

There are currently a few task support implementations available for parallel
collections. The `ForkJoinTaskSupport` uses a fork-join pool internally and is
used by default on JVM 1.6 or greater. The less efficient
`ThreadPoolTaskSupport` is a fallback for JVM 1.5 and JVMs that do not support
the fork join pools. The `ExecutionContextTaskSupport` uses the default
execution context implementation found in `scala.concurrent`, and it reuses
the thread pool used in `scala.concurrent` (this is either a fork join pool or
a thread pool executor, depending on the JVM version). The execution context
task support is set to each parallel collection by default, so parallel
collections reuse the same fork-join pool as the future API.

Here is a way to change the task support of a parallel collection:

    scala> import scala.collection.parallel._
    import scala.collection.parallel._
    
    scala> val pc = mutable.ParArray(1, 2, 3)
    pc: scala.collection.parallel.mutable.ParArray[Int] = ParArray(1, 2, 3)
    
    scala> pc.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2))
    pc.tasksupport: scala.collection.parallel.TaskSupport = scala.collection.parallel.ForkJoinTaskSupport@4a5d484a
    
    scala> pc map { _ + 1 }
    res0: scala.collection.parallel.mutable.ParArray[Int] = ParArray(2, 3, 4)

The above sets the parallel collection to use a fork-join pool with
parallelism level 2. To set the parallel collection to use a thread pool
executor:

    scala> pc.tasksupport = new ThreadPoolTaskSupport()
    pc.tasksupport: scala.collection.parallel.TaskSupport = scala.collection.parallel.ThreadPoolTaskSupport@1d914a39
    
    scala> pc map { _ + 1 }
    res1: scala.collection.parallel.mutable.ParArray[Int] = ParArray(2, 3, 4)

When a parallel collection is serialized, the task support field is omitted
from serialization. When deserializing a parallel collection, the task support
field is set to the default value-- the execution context task support.

To implement a custom task support, extend the `TaskSupport` trait and
implement the following methods:

    def execute[R, Tp](task: Task[R, Tp]): () => R
    
    def executeAndWaitResult[R, Tp](task: Task[R, Tp]): R
    
    def parallelismLevel: Int

The `execute` method schedules a task asynchronously and returns a future to
wait on the result of the computation. The `executeAndWait` method does the
same, but only returns when the task is completed. The `parallelismLevel`
simply returns the targeted number of cores that the task support uses to
schedule tasks.


## References

1. [On a Generic Parallel Collection Framework, June 2011][1]

  [1]: http://infoscience.epfl.ch/record/165523/files/techrep.pdf "parallel-collections"
