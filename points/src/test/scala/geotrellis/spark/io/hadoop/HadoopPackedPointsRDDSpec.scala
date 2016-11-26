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

package geotrellis.spark.io.hadoop

import geotrellis.spark.PointsTestEnvironment
import org.scalatest._

class HadoopPackedPointsRDDSpec extends FunSpec
  with Matchers
  with PointsTestEnvironment {
  describe("PackedPoints RDD reads") {
    it("should read LAS file as RDD using hadoop input format") {
      val source = HadoopPackedPointsRDD(lasPath)
      val sourceList = source.take(1).toList
      sourceList.map { case (k, _) => k.crs.proj4jCrs.getName }.head should be ("lcc-CS")
      sourceList.map { case (_, v) => v.length }.head should be (1065)
    }
  }
}
