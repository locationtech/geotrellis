---
layout: overview-large
title: Measuring Performance

disqus: true

partof: parallel-collections
num: 8
outof: 8
languages: [ja, es]
---

## Performance on the JVM

The performance model on the JVM is sometimes convoluted in commentaries about
it, and as a result is not well understood. For various reasons, some code may
not be as performant or as scalable as expected. Here, we provide a few
examples.

One of the reasons is that the compilation process for a JVM application is
not the same as that of a statically compiled language (see \[[2][2]\]). The
Java and Scala compilers convert source code into JVM bytecode and do very
little optimization. On most modern JVMs, once the program bytecode is run, it
is converted into machine code for the computer architecture on which it is
being run. This is called the just-in-time compilation. The level of code
optimization is, however, low with just-in-time compilation, since it has to
be fast. To avoid recompiling, the so called HotSpot compiler only optimizes
parts of the code which are executed frequently. What this means for the
benchmark writer is that a program might have different  performance each time
it is run. Executing the same piece of code (e.g. a method) multiple times in
the same JVM instance might give very different performance results depending
on whether the particular code was optimized in between the runs.
Additionally, measuring the execution time of some piece of code may include
the time during which the JIT compiler itself was performing the optimization,
thus giving inconsistent results.

Another hidden execution that takes part on the JVM is the automatic memory
management. Every once in a while, the execution of the program is stopped and
a garbage collector is run. If the program being benchmarked allocates any
heap memory at all (and most JVM programs do), the garbage collector will have
to run, thus possibly distorting the measurement. To amortize the garbage
collection effects, the measured program should run many times to trigger many
garbage collections.

One common cause of a performance deterioration is also boxing and unboxing
that happens implicitly when passing a primitive type as an argument to a
generic method. At runtime, primitive types are converted to objects which
represent them, so that they could be passed to a method with a generic type
parameter. This induces extra allocations and is slower, also producing
additional garbage on the heap.

Where parallel performance is concerned, one common issue is memory
contention, as the programmer does not have explicit control about where the
objects are allocated.
In fact, due to GC effects, contention can occur at a later stage in
the application lifetime after objects get moved around in memory.
Such effects need to be taken into consideration when writing a benchmark.


## Microbenchmarking example

There are several approaches to avoid the above effects during measurement.
First of all, the target microbenchmark must be executed enough times to make
sure that the just-in-time compiler compiled it to machine code and that it
was optimized. This is known as the warm-up phase.

The microbenchmark itself should be run in a separate JVM instance to reduce
noise coming from garbage collection of the objects allocated by different
parts of the program or unrelated just-in-time compilation.

It should be run using the server version of the HotSpot JVM, which does more
aggressive optimizations.

Finally, to reduce the chance of a garbage collection occurring in the middle
of the benchmark, ideally a garbage collection cycle should occur prior to the
run of the benchmark, postponing the next cycle as far as possible.

The `scala.testing.Benchmark` trait is predefined in the Scala standard
library and is designed with above in mind. Here is an example of benchmarking
a map operation on a concurrent trie:

    import collection.parallel.mutable.ParTrieMap
	import collection.parallel.ForkJoinTaskSupport
	
    object Map extends testing.Benchmark {
      val length = sys.props("length").toInt
      val par = sys.props("par").toInt
      val partrie = ParTrieMap((0 until length) zip (0 until length): _*)
      
      partrie.tasksupport = new ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(par))
      
      def run = {
        partrie map {
          kv => kv
        }
      }
    }

The `run` method embodies the microbenchmark code which will be run
repetitively and whose running time will be measured. The object `Map` above
extends the `scala.testing.Benchmark` trait and parses system specified
parameters `par` for the parallelism level and `length` for the number of
elements in the trie.

After compiling the program above, run it like this:

    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=1 -Dlength=300000 Map 10

The `server` flag specifies that the server VM should be used. The `cp`
specifies the classpath and includes classfiles in the current directory and
the scala library jar. Arguments `-Dpar` and `-Dlength` are the parallelism
level and the number of elements. Finally, `10` means that the benchmark
should be run that many times within the same JVM.

Running times obtained by setting the `par` to `1`, `2`, `4` and `8` on a
quad-core i7 with hyperthreading:

    Map$	126	57	56	57	54	54	54	53	53	53
    Map$	90	99	28	28	26	26	26	26	26	26
    Map$	201	17	17	16	15	15	16	14	18	15
    Map$	182	12	13	17	16	14	14	12	12	12

We can see above that the running time is higher during the initial runs, but
is reduced after the code gets optimized. Further, we can see that the benefit
of hyperthreading is not high in this example, as going from `4` to `8`
threads results only in a minor performance improvement.


## How big should a collection be to go parallel?

This is a question commonly asked. The answer is somewhat involved.

The size of the collection at which the parallelization pays of really
depends on many factors. Some of them, but not all, include:

- Machine architecture. Different CPU types have different
  performance and scalability characteristics. Orthogonal to that,
  whether the machine is multicore or has multiple processors
  communicating via motherboard.
- JVM vendor and version. Different VMs apply different
  optimizations to the code at runtime. They implement different memory
  management and synchronization techniques. Some do not support
  `ForkJoinPool`, reverting to `ThreadPoolExecutor`s, resulting in
  more overhead.
- Per-element workload. A function or a predicate for a parallel
  operation determines how big is the per-element workload. The
  smaller the workload, the higher the number of elements needed to
  gain speedups when running in parallel.
- Specific collection. For example, `ParArray` and
  `ParTrieMap` have splitters that traverse the collection at
  different speeds, meaning there is more per-element work in just the
  traversal itself.
- Specific operation. For example, `ParVector` is a lot slower for
  transformer methods (like `filter`) than it is for accessor methods (like `foreach`)
- Side-effects. When modifying memory areas concurrently or using
  synchronization within the body of `foreach`, `map`, etc.,
  contention can occur.
- Memory management. When allocating a lot of objects a garbage
  collection cycle can be triggered. Depending on how the references
  to new objects are passed around, the GC cycle can take more or less time.

Even in separation, it is not easy to reason about things above and
give a precise answer to what the collection size should be. To
roughly illustrate what the size should be, we give an example of
a cheap side-effect-free parallel vector reduce (in this case, sum)
operation performance on an i7 quad-core processor (not using
hyperthreading) on JDK7:

    import collection.parallel.immutable.ParVector
    
    object Reduce extends testing.Benchmark {
      val length = sys.props("length").toInt
      val par = sys.props("par").toInt
      val parvector = ParVector((0 until length): _*)
      
      parvector.tasksupport = new collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(par))
      
      def run = {
        parvector reduce {
          (a, b) => a + b
        }
      }
    }
   
    object ReduceSeq extends testing.Benchmark {
      val length = sys.props("length").toInt
      val vector = collection.immutable.Vector((0 until length): _*)
      
      def run = {
        vector reduce {
          (a, b) => a + b
        }
      }
    }

We first run the benchmark with `250000` elements and obtain the
following results, for `1`, `2` and `4` threads:

    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=1 -Dlength=250000 Reduce 10 10
    Reduce$    54    24    18    18    18    19    19    18    19    19
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=2 -Dlength=250000 Reduce 10 10
    Reduce$    60    19    17    13    13    13    13    14    12    13
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=4 -Dlength=250000 Reduce 10 10
    Reduce$    62    17    15    14    13    11    11    11    11    9

We then decrease the number of elements down to `120000` and use `4`
threads to compare the time to that of a sequential vector reduce:

    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=4 -Dlength=120000 Reduce 10 10
    Reduce$    54    10    8    8    8    7    8    7    6    5
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dlength=120000 ReduceSeq 10 10
    ReduceSeq$    31    7    8    8    7    7    7    8    7    8

`120000` elements seems to be the around the threshold in this case.

As another example, we take the  `mutable.ParHashMap` and the `map`
method (a transformer method) and run the following benchmark in the same environment:

    import collection.parallel.mutable.ParHashMap
    
    object Map extends testing.Benchmark {
      val length = sys.props("length").toInt
      val par = sys.props("par").toInt
      val phm = ParHashMap((0 until length) zip (0 until length): _*)
      
      phm.tasksupport = new collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(par))
      
      def run = {
        phm map {
          kv => kv
        }
      }
    }
   
    object MapSeq extends testing.Benchmark {
      val length = sys.props("length").toInt
      val hm = collection.mutable.HashMap((0 until length) zip (0 until length): _*)
      
      def run = {
        hm map {
          kv => kv
        }
      }
    }

For `120000` elements we get the following times when ranging the
number of threads from `1` to `4`:

    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=1 -Dlength=120000 Map 10 10    
    Map$    187    108    97    96    96    95    95    95    96    95
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=2 -Dlength=120000 Map 10 10
    Map$    138    68    57    56    57    56    56    55    54    55
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=4 -Dlength=120000 Map 10 10
    Map$    124    54    42    40    38    41    40    40    39    39

Now, if we reduce the number of elements to `15000` and compare that
to the sequential hashmap:

    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=1 -Dlength=15000 Map 10 10
    Map$    41    13    10    10    10    9    9    9    10    9
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dpar=2 -Dlength=15000 Map 10 10
    Map$    48    15    9    8    7    7    6    7    8    6
    java -server -cp .:../../build/pack/lib/scala-library.jar -Dlength=15000 MapSeq 10 10
    MapSeq$    39    9    9    9    8    9    9    9    9    9

For this collection and this operation it makes sense
to go parallel when there are above `15000` elements (in general,
it is feasible to parallelize hashmaps and hashsets with fewer
elements than would be required for arrays or vectors).





## References

1. [Anatomy of a flawed microbenchmark, Brian Goetz][1]
2. [Dynamic compilation and performance measurement, Brian Goetz][2]

  [1]: http://www.ibm.com/developerworks/java/library/j-jtp02225/index.html "flawed-benchmark"
  [2]: http://www.ibm.com/developerworks/library/j-jtp12214/ "dynamic-compilation"



