/*
 * Copyright 2017 Azavea & Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.index

import org.openjdk.jmh.annotations._

import scala.util.Random

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class MergeQueueBench {

  // Whether to shuffle the ranges or leave them sorted
  @Param(Array("false", "true"))
  var shuffle: Boolean = _
  @Param(Array("100000", "10000"))
  // Number of ranges to insert
  var size: Int = _
  // How far each range is from the previous
  @Param(Array("10", "1"))
  var skip: Int = _
  // How wide each range is
  @Param(Array("10", "1"))
  var span: Int = _

  var ranges: Vector[(BigInt, BigInt)] = _

  @Setup(Level.Trial)
  def setupData(): Unit = {
    ranges = Vector.empty[(BigInt, BigInt)]

    for(i ← 0 until size) {
      val start: BigInt = BigInt(skip.toLong * i)
      val end: BigInt = start + span
      ranges = ranges :+ (start, end)
    }

    if(shuffle) {
      ranges = Random.shuffle(ranges)
    }
  }

  @Benchmark
  def insertRange(): Seq[(BigInt, BigInt)] = MergeQueue(ranges)

  @Benchmark
  def insertAlternateRange(): Seq[(Long, Long)] =
    MergeQueueBench.AlternateMergeQueue(ranges.map { case (a, b) => (a.longValue, b.longValue) })
}

object MergeQueueBench {
  object AlternateMergeQueue {
    def apply(ranges: TraversableOnce[(Long, Long)]): Seq[(Long, Long)] = {
      var merged: List[(Long, Long)] = Nil
      ranges.toSeq.sortBy(_._1).foreach(r => {
        merged = merged match {
          case a :: rest if r._1 - 1 <= a._2 => (a._1, Math.max(a._2, r._2)) :: rest
          case _ => r :: merged
        }
      })
      merged.reverse
    }
  }
}
