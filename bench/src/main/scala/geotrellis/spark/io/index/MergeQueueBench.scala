package geotrellis.spark.io.index

import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class MergeQueueBench {

  var queue: MergeQueue = _

  @Setup
  def setup() = {
    queue = new MergeQueue()
  }

  @TearDown
  def tearDown() = {
    queue = null
  }

  @Benchmark
  def mergeOrderedDense  = {
    for(i ← -10000 to 10000) {
      queue += (i, i + 10)
    }
    queue
  }
}
