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
import scalaxy.loops._
import scala.collection.mutable
import geotrellis.raster.IntArrayRasterData

abstract sealed trait Connectivity
case object FourNeighbors extends Connectivity
case object EightNeighbors extends Connectivity

case class RegionGroupResult(raster:Raster,regionMap:Map[Int,Int])

object RegionGroupOptions { val default = RegionGroupOptions() }
case class RegionGroupOptions(
  connectivity:Connectivity = FourNeighbors,
  ignoreNoData:Boolean = true
)

case class RegionGroup(r:Op[Raster], options:RegionGroupOptions = RegionGroupOptions.default) 
extends Op1(r)({
  r =>
    var regionId = -1
    val regions = new RegionPartition()
    val regionMap = mutable.Map[Int,Int]()
    val cols = r.cols
    val rows = r.rows
    val data = IntArrayRasterData.empty(cols,rows)
    val ignoreNoData = options.ignoreNoData

    /* Go through each cell row by row, and set all considered neighbors
     * whose values are equal to the current cell's values to be of the 
     * same region. If there are no equal considered neighbors, then the cell
     * represents a new region. Considered neighbors are: west and north, for 
     * connectivity of FourNieghbors, and west, north, and northwest for connectivity
     * of EightNeighbors.
     * 
     * Code is duplicated for Four and Eight Neighbor connectivity for performance
     * and to try and avoid a mess of conditionals.
     */
    if(options.connectivity == FourNeighbors) {
      for (row <- 0 until rows optimized) {
        var valueToLeft = NODATA
        for (col <- 0 until cols optimized) {
          val v = r.get(col,row)
          if(isData(v) || !ignoreNoData) {
            val top =
              if(row > 0) { r.get(col,row - 1) }
              else { v + 1 }

            if(v == top) {
              // Value to north is the same region
              val topRegion = data.get(col,row-1)
              data.set(col,row,topRegion)

              if(v == valueToLeft && col > 0) {
                // Value to west is also same region
                val leftRegion = data.get(col-1,row)
                if(leftRegion != topRegion) {
                  // Set the north and west regions equal
                  regions.add(topRegion,data.get(col-1,row))
                }
              }
            } else {
              if(v == valueToLeft && col > 0) {
                // Value to west is same region
                data.set(col,row,data.get(col-1,row))
              } else {
                // This value represents a new region
                regionId += 1
                regions.add(regionId)
                data.set(col,row,regionId)
                regionMap(regionId) = v
              }
            }
          }
          valueToLeft = v
        }
      }
    } else {
      for (row <- 0 until rows optimized) {
        var valueToLeft = NODATA
        var valueToTopLeft = NODATA
        var valueToTop = NODATA
        for (col <- 0 until cols optimized) {
          val v = r.get(col,row)
          val topRight =
            if(row > 0 && col < cols-1) { r.get(col+1,row - 1) }
            else { v + 1 }

          if(col == 0) {
            if(row > 0) { valueToTop = r.get(col,row - 1) }
            else { valueToTop = v + 1 }
          }

          if(isData(v) || !ignoreNoData) {
            if(v == topRight) {
              // Value to north east is the same region
              val topRightRegion = data.get(col+1,row-1)
              data.set(col,row,topRightRegion)
              if(v == valueToLeft && col > 0) {
                // Value to west is also same region
                val leftRegion = data.get(col-1,row)
                if(leftRegion != topRightRegion) {
                  // Set the north and west regions equal
                  regions.add(topRightRegion,data.get(col-1,row))
                }
              }
              if(v == valueToTopLeft && col > 0 && row > 0) {
                // Value to northwest is also same region
                val topLeftRegion = data.get(col-1,row-1)
                if(topLeftRegion != topRightRegion) {
                  // Set the north and west regions equal
                  regions.add(topRightRegion,data.get(col-1,row-1))
                }
              }
              if(v == valueToTop && row > 0) {
                // Value to north is also same region
                val topRegion = data.get(col,row-1)
                if(topRegion != topRightRegion) {
                  // Set the north and west regions equal
                  regions.add(topRightRegion,data.get(col,row-1))
                }
              }
            } else {
              if(v == valueToTop) {
                // Value to north east is the same region
                val topRegion = data.get(col,row-1)
                data.set(col,row,topRegion)
                if(v == valueToLeft && col > 0) {
                  // Value to west is also same region
                  val leftRegion = data.get(col-1,row)
                  if(leftRegion != topRegion) {
                    // Set the north and west regions equal
                    regions.add(topRegion,data.get(col-1,row))
                  }
                }
                if(v == valueToTopLeft && col > 0 && row > 0) {
                  // Value to northwest is also same region
                  val topLeftRegion = data.get(col-1,row-1)
                  if(topLeftRegion != topRegion) {
                    // Set the north and west regions equal
                    regions.add(topRegion,data.get(col-1,row-1))
                  }
                }
              } else {
                if(v == valueToLeft && col > 0) {
                  // Value to west is same region
                  val westRegion = data.get(col-1,row)
                  data.set(col,row,westRegion)
                  if(v == valueToTopLeft && col > 0 && row > 0) {
                    // Value to northwest is also same region
                    val topLeftRegion = data.get(col-1,row-1)
                    if(topLeftRegion != westRegion) {
                      // Set the north and west regions equal
                      regions.add(westRegion,data.get(col-1,row-1))
                    }
                  }
                } else {
                  if(v == valueToTopLeft && col > 0 && row > 0) {
                    data.set(col,row,data.get(col-1,row-1))
                  } else {
                    // This value represents a new region
                    regionId += 1
                    regions.add(regionId)
                    data.set(col,row,regionId)
                    regionMap(regionId) = v
                  }
                }
              }
            }
          }
          valueToLeft = v
          valueToTopLeft = valueToTop
          valueToTop = topRight
        }
      }
    }

    // Set equivilant regions to minimum
    
    for (row <- 0 until rows optimized) {
      for (col <- 0 until cols optimized) {
        val v = data.get(col,row)
        if(isData(v) || !ignoreNoData) { 
          val cls = regions.getClass(v)
          if(cls != v && regionMap.contains(v)) {
            if(!regionMap.contains(cls)) {
              sys.error(s"Region map should contain class identifier $cls")
            }
            regionMap.remove(v) 
          }
          data.set(col,row,regions.getClass(v))
        }
      }
    }

  Result(RegionGroupResult(Raster(data,r.rasterExtent),regionMap.toMap))
})

class RegionPartition() {
  val regionMap = mutable.Map[Int,Partition]()

  object Partition {
    def apply(x:Int) = {
      val p = new Partition
      p += x
      p
    }

    def apply(x:Int,y:Int) = {
      val p = new Partition
      p += x
      p += y
      p
    }
  }

  class Partition() {
    val members = mutable.Set[Int]()
    var _min = Int.MaxValue

    def min = _min

    def +=(x:Int) = {
      regionMap(x) = this
      members += x
      if(x < _min) { _min = x }
    }

    def absorb(other:Partition) =
      if(this != other) {
        for(m <- other.members) { +=(m) }
      }

    override def toString = s"PARTITION($min)"
  }

  def add(x:Int) =
    if(!regionMap.contains(x)) {
      Partition(x)
    }

  def add(x:Int,y:Int):Unit = {
    if(!regionMap.contains(x)) {
      if(!regionMap.contains(y)) {
        // x and y are not mapped
        Partition(x,y)
      } else {
        // x is not mapped, y is mapped
        regionMap(y) += x
      }
    } else {
      if(!regionMap.contains(y)) {
        // x is mapped, y is not mapped
        regionMap(x) += y
      } else {
        regionMap(x).absorb(regionMap(y))
      }
    }
  }

  def getClass(x:Int) = 
    if(!regionMap.contains(x)) { sys.error(s"There is no partition containing $x") }
    else {
      regionMap(x).min
    }
}
