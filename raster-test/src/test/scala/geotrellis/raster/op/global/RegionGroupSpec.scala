/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.testkit._
import geotrellis.statistics.op.stat._

import scala.collection.mutable

import org.scalatest._

class RegionGroupSpec extends FunSpec
                         with TestServer
                         with RasterBuilders {
  describe("RegionGroup") {
    it("should group regions.") {
      val r = createRaster(
        Array(NODATA,NODATA,NODATA,NODATA,     9,
              NODATA,NODATA,     9,     9,     9,
              NODATA,     9,NODATA,     5,     5,
              NODATA,     9,     9,NODATA,NODATA,
              NODATA,NODATA,     9,     9,NODATA,
                   7,     7,NODATA,     9,     9),
          5, 6
      )
      val regions = get(RegionGroup(r)).raster

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (4)

    }

    it("should group regions with a connectivity of 8 neighbors.") {
      val r = createRaster(
        Array(NODATA,NODATA,NODATA,NODATA,     9,
              NODATA,NODATA,     9,     9,     9,
              NODATA,     9,NODATA,     5,     5,
              NODATA,     9,     9,NODATA,NODATA,
              NODATA,NODATA,     9,     9,NODATA,
                   7,     7,NODATA,     9,     9),
          5, 6
      )
      val options = RegionGroupOptions(connectivity = EightNeighbors)
      val regions = get(RegionGroup(r,options)).raster

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (3)

    }

    it("should group regions on larger example.") {
      val n = NODATA
      val r = createRaster(
        Array( n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
               n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
               n, n, n, 1, 1, n, n, n, n, n, 7, n, n, 7, n, n, n,
               n, n, n, 1, 1, n, n, n, n, n, 7, 7, n, 7, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 7, n, n, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 7, 7, 7, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, n, n, 7, n, n, n,
               n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n,
               n, n, n, n, 3, n, n, 3, n, n, n, n, n, n, n, n, n,
               n, n, 3, 3, 3, 3, 3, 3, 3, 3, n, n, n, n, n, n, n,
               n, n, n, n, n, n, 5, n, n, 3, 3, n, n, n, n, 8, 8,
               n, n, n, n, n, n, 5, n, n, n, n, n, n, n, n, 8, n,
               n, n, n, n, n, n, 5, n, n, n, n, n, n, 8, 8, 8, n,
               n, n, n, n, n, n, 5, 5, n, n, n, n, n, 8, n, n, n,
               n, n, n, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
               n, n, 5, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
               n, 5, 5, 5, 5, 5, 5, n, n, 5, 5, n, n, n, n, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 5, 5, n, n, n, n),
               17,18
      )

      val RegionGroupResult(regions,regionMap) = get(RegionGroup(r))

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (8)
      
      val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
      for (col <- 0 until 17) {
        for (row <- 0 until 18) {
          val v = r.get(col,row)
          val region = regions.get(col,row)

          if(isNoData(v)) { region should be (v) }
          else {
            regionMap(region) should be (v)
            if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
            regionCounts(v) += region
          }
        }
      }

      regionCounts(1).size should be (2)
      regionCounts(7).size should be (1)
      regionCounts(3).size should be (1)
      regionCounts(8).size should be (1)
      regionCounts(5).size should be (3)
    }

    it("should group regions on larger example without ignoringNoData.") {
      val n = NODATA
      val r = createRaster(
        Array( n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
               n, 1, 1, n, n, n, n, n, n, n, 7, 7, n, n, n, n, n,
               n, n, n, 1, 1, n, n, n, n, n, 7, n, n, 7, n, n, n,
               n, n, n, 1, 1, n, n, n, n, n, 7, 7, n, 7, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 7, n, n, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 7, 7, 7, 7, n, n,
               n, n, n, n, n, n, n, n, n, n, n, n, n, 7, n, n, n,
               n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n, n,
               n, n, n, n, 3, n, n, 3, n, n, n, n, n, n, n, n, n,
               n, n, 3, 3, 3, 3, 3, 3, 3, 3, n, n, n, n, n, n, n,
               n, n, n, n, n, n, 5, n, n, 3, 3, n, n, n, n, 8, 8,
               n, n, n, n, n, n, 5, n, n, n, n, n, n, n, n, 8, n,
               n, n, n, n, n, n, 5, n, n, n, n, n, n, 8, 8, 8, n,
               n, n, n, n, n, n, 5, 5, n, n, n, n, n, 8, n, n, n,
               n, n, n, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
               n, n, 5, 5, 5, n, 5, n, n, n, n, n, n, n, n, n, n,
               n, 5, 5, 5, 5, 5, 5, n, n, 5, 5, n, n, n, n, n, n,
               n, n, n, n, n, n, n, n, n, n, n, 5, 5, n, n, n, n),
               17,18
      )

      val RegionGroupResult(regions,regionMap) = 
        get(RegionGroup(r,RegionGroupOptions(ignoreNoData = false)))

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (9)
      
      val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
      for (col <- 0 until 17) {
        for (row <- 0 until 18) {
          val v = r.get(col,row)
          val region = regions.get(col,row)

          regionMap(region) should be (v)
          if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
          regionCounts(v) += region
        }
      }

      regionCounts(NODATA).size should be (1)
      regionCounts(1).size should be (2)
      regionCounts(7).size should be (1)
      regionCounts(3).size should be (1)
      regionCounts(8).size should be (1)
      regionCounts(5).size should be (3)
    }

    it("should group regions when regions are concentric.") {
      val n = NODATA
      val r = createRaster(
        Array(
               n, n, 7, 7, 7, n, n,
               n, 7, 7, 5, 7, 7, n,
               7, 7, 5, 5, 5, 7, 7,
               7, 5, 5, 9, 5, 5, 7,
               7, 5, 5, 7, 5, 7, 7,
               7, 7, 5, 5, 5, 7, n,
               n, 7, 5, 5, 7, 7, n,
               n, 7, 7, 7, 7, n, n
        ),
               7,8
      )

      val RegionGroupResult(regions,regionMap) = get(RegionGroup(r))

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (4)
      
      val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
      for (col <- 0 until 7) {
        for (row <- 0 until 8) {
          val v = r.get(col,row)
          val region = regions.get(col,row)
          if(isNoData(v)) { region should be (v) }
          else {
            regionMap(region) should be (v)
            if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
            regionCounts(v) += region
          }
        }
      }

      regionCounts(7).size should be (2)
      regionCounts(9).size should be (1)
      regionCounts(5).size should be (1)
    }

    it("should group regions when region has NODATA holes.") {
      val n = NODATA
      val r = createRaster(
        Array(
               n, n, n, n, n, n, n,
               n, n, 5, 5, 5, n, n,
               n, 5, 5, n, 5, 5, n,
               n, 5, n, n, n, 5, n,
               n, 5, n, n, n, 5, n,
               n, 5, 5, n, 5, 5, n,
               n, n, 5, 5, 5, n, n,
               n, n, n, n, n, n, n
        ),
               7,8
      )

      val RegionGroupResult(regions,regionMap) = get(RegionGroup(r))

      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (1)
      
      for (col <- 0 until 7) {
        for (row <- 0 until 8) {
          val v = r.get(col,row)
          val region = regions.get(col,row)
          if(isNoData(v)) { region should be (v) }
          else { region should be (0) }
        }
      }
    }

    it("should count regions with a nodata line almost separating regions") {
      val n = NODATA
      val arr = 
        Array(
//             0  1  2  3  4 
               1, 1, 1, 1, n,// 0
               1, 5, 5, 1, n,// 1
               5, 5, 1, 1, 1,// 2
               n, n, n, n, 1,// 3
               1, n, 1, n, 1,// 4
               1, 1, 1, 5, 1,// 5
               1, 5, 5, 1, 1,// 6
               1, 1, 1, 1, n)// 7

      val cw = 1
      val ch = 10
      val cols = 5
      val rows = 8
      val xmin = 0
      val xmax = 5
      val ymin = -70
      val ymax = 0

      val r = Raster(arr,RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows))
      val RegionGroupResult(regions,regionMap) = get(RegionGroup(r))
      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (4)
      
      val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
      for (col <- 0 until 5) {
        for (row <- 0 until 8) {
          val v = r.get(col,row)
          val region = regions.get(col,row)
          if(isNoData(v)) { region should be (v) }
          else {
            regionMap(region) should be (v)
            if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
            regionCounts(v) += region
          }
        }
      }
    }

    it("should count regions with a nodata line almost separating regions without ignoring NODATA") {
      val n = NODATA
      val arr = 
        Array(
//             0  1  2  3  4 
               1, 1, 1, 1, n,// 0
               1, 5, 5, 1, n,// 1
               5, 5, 1, 1, 1,// 2
               n, n, n, n, 1,// 3
               1, n, 1, n, 1,// 4
               1, 1, 1, 5, 1,// 5
               1, 5, 5, 1, 1,// 6
               1, 1, 1, 1, n)// 7

      val cw = 1
      val ch = 10
      val cols = 5
      val rows = 8
      val xmin = 0
      val xmax = 5
      val ymin = -70
      val ymax = 0

      val r = Raster(arr,RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows))
      val RegionGroupResult(regions,regionMap) = 
        get(RegionGroup(r,RegionGroupOptions(ignoreNoData = false)))
      val histogram = get(GetHistogram(regions))
      val count = histogram.getValues.length
      count should be (7)
      
      val regionCounts = mutable.Map[Int,mutable.Set[Int]]()
      for (col <- 0 until 5) {
        for (row <- 0 until 8) {
          val v = r.get(col,row)
          val region = regions.get(col,row)
          regionMap(region) should be (v)
          if(!regionCounts.contains(v)) { regionCounts(v) = mutable.Set[Int]() }
          regionCounts(v) += region
        }
      }
    }
  }
}


class RegionPartitionSpec extends FunSpec
                             with TestServer
                             with RasterBuilders {
  describe("RegionPartition") {
    it("should work in a once problematic case") {
      val rp = new RegionPartition()

      rp.add(60,48)

      rp.add(63,62)
      rp.add(60,62)

      rp.add(60,23)

      rp.getClass(63) should be (23)
    }
  }
}
