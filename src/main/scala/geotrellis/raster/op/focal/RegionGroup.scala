package geotrellis.raster.op.focal

import geotrellis._

import spire.syntax._

import scala.collection.mutable

case class RegionGroupResult(raster:Raster,regionMap:Map[Int,Int])

case class RegionGroup(r:Op[Raster]) extends Op1(r)({
  r =>
    var regionId = -1
    val regions = new RegionPartition()
    val regionMap = mutable.Map[Int,Int]()
    val cols = r.cols
    val rows = r.rows
    val data = IntArrayRasterData.empty(cols,rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      var valueToLeft = NODATA
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = r.get(col,row)
        if(v != NODATA) {
          val top =
            if(row > 0) { r.get(col,row - 1) }
            else { v + 1 }

          if(v == top) {
            // Value to north is the same region
            val topRegion = data.get(col,row-1)
            if(v == valueToLeft) {
              // Value to west is also same region
              val leftRegion = data.get(col-1,row)
              if(leftRegion != topRegion) {
                // Set the north and west regions equal
                regions.add(topRegion,data.get(col-1,row))
              }
              data.set(col,row,topRegion)
            } else {
              data.set(col,row,topRegion)
            }
            if(!regionMap.contains(topRegion)) { regionMap(topRegion) = v }
          } else {
            if(v == valueToLeft) {
              // Value to west is same region
              val westRegion = data.get(col-1,row)
              data.set(col,row,westRegion)
              if(!regionMap.contains(westRegion)) { regionMap(westRegion) = v }
            } else {
              // This value represents a new region
              regionId += 1
              regions.add(regionId)
              data.set(col,row,regionId)
              if(!regionMap.contains(regionId)) { regionMap(regionId) = v }
            }
          }
        }
        valueToLeft = v
      }
    }

    // Set equivilant regions to minimum
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = data.get(col,row)
        if(v != NODATA) { 
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
  private val regionMap = mutable.Map[Int,Partition]()

  object Partition {
    def apply(ms:Int*) = {
      val p = new Partition
      p += (ms:_*)
      p
    }
  }

  class Partition() {
    val members = mutable.Set[Int]()
    var _min = Int.MaxValue

    def min = _min

    def +=(ms:Int*) = {
      for(m <- ms) {
        regionMap(m) = this
        members += m
        if(m < _min) { _min = m }
      }
    }

    def absorb(other:Partition) = +=(other.members.toSeq:_*)

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
