/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles.{IncreasingTestFile, AllOnesTestFile}

import geotrellis.vector.Extent

import org.scalatest.FunSpec

class SumSpec extends FunSpec
    with TestEnvironment
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {

  def getSubExtent(
    extent: Extent,
    startX: Double = 0,
    startY: Double = 0,
    endX: Double = 1,
    endY: Double = 1): Extent = {
    val dx = extent.xmax - extent.xmin
    val dy = extent.ymax - extent.ymin
    val xmin = extent.xmin + startX * dx
    val ymin = extent.ymin + startY * dy
    val xmax = extent.xmax - (1 - endX) * dx
    val ymax = extent.ymax - (1 - endY) * dy

    Extent(xmin, ymin, xmax, ymax)
  }

  describe("Sum Zonal Summary Operation") {
    ifCanRunSpark {
      val allOnes = AllOnesTestFile(inputHome, conf)
      val increasing = IncreasingTestFile(inputHome, conf)

      val cols = allOnes.metaData.cols
      val rows = allOnes.metaData.rows
      val tileTotal = cols * rows
      val count = allOnes.metaData.count
      val rddTotal = count * tileTotal
      val extent = allOnes.metaData.extent
      println(extent)

      it("should compute sum with all ones and whole extent") {
        val ones = sc.hadoopRasterRDD(allOnes.path)
        val polygon = extent.toPolygon

        val result = ones.zonalSum(polygon).collect

        result.size should equal (1)
        val histogram = result.head

        histogram should equal (rddTotal)
      }
    }
  }
}
