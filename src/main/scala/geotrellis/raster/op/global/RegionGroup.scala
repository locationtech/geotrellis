package geotrellis.raster.op.global

import geotrellis._
import spire.syntax._
import scala.collection.mutable
import geotrellis.raster.IntArrayRasterData

case class RegionGroupResult(raster:Raster,regionMap:Map[Int,Int])

object RegionGroupOptions { val Default = RegionGroupOptions() }
case class RegionGroupOptions(ignoreNoData:Boolean = true)

case class RegionGroup(r:Op[Raster], options:RegionGroupOptions = RegionGroupOptions.Default) 
extends Op1(r)({
  r =>
    var regionId = -1
    val regions = new RegionPartition()
    val regionMap = mutable.Map[Int,Int]()
    val cols = r.cols
    val rows = r.rows
    val data = IntArrayRasterData.empty(cols,rows)
    val ignoreNoData = options.ignoreNoData
    cfor(0)(_ < rows, _ + 1) { row =>
      var valueToLeft = NODATA
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = r.get(col,row)
        if(v != NODATA || !ignoreNoData) {
          val top =
            if(row > 0) { r.get(col,row - 1) }
            else { v + 1 }

          if(v == top) {
            // Value to north is the same region
            val topRegion = data.get(col,row-1)
            if(v == valueToLeft && col > 0) {
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
            if(v == valueToLeft && col > 0) {
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
        if(v != NODATA || !ignoreNoData) { 
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
