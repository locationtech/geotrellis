package geotrellis.spark.io.index


import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@Threads(1)
@State(Scope.Thread)
class MergeQueueBench {
  val queue = new MergeQueue()

  @Benchmark
  def mergeOrderedDense  = {
    for(i ← -10000 to 10000) {
      queue += (i, i + 10)
    }
    queue
  }
}
