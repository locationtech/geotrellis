/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.util

import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.TileLayerRDD
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class OptimusPrime(val prime: Int) extends Function1[Int, Int] {
  def apply(x: Int): Int = prime + x
}
class KryoClosureSpec extends FunSpec with TestEnvironment {
  val transformer = new OptimusPrime(7)
  val numbers = Array.fill(10)(10)

  describe("KryoClosure") {
    val rdd = sc.parallelize(numbers, 1)

    it("should be better then Java serialization") {
      intercept[org.apache.spark.SparkException] {
        rdd.map(transformer).collect
      }
    }

    it("should be totally awesome at serialization"){
      val out = rdd.map(KryoClosure(transformer))
      out.collect should be (Array.fill(10)(17))
    }
  }
}
