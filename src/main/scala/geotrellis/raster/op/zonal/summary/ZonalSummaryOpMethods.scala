package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._
import geotrellis.statistics._

trait ZonalSummaryOpMethods[+Repr <: RasterDataSource] { self:Repr =>
  def zonalHistogram[D](p:Op[feature.Polygon[D]]):ValueDataSource[Histogram] = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          val histogram = FastMapHistogram()
          r.force.foreach((z:Int) => if (z != NODATA) histogram.countItem(z, 1))
          histogram
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          val histogram = FastMapHistogram()
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply (col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (z != NODATA) histogram.countItem(z, 1)
                }
              }
            )
          }

          histogram
      }
    }.converge

  def zonalSum[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var s = 0L
          r.foreach((x:Int) => if (x != NODATA) s = s + x)
          s
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var sum: Long = 0L
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (z != NODATA) { sum = sum + z }
                }
              }
            )
          }

          sum
      }
    }.reduce(_+_)

  def zonalSumDouble[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var s = 0.0
          r.foreachDouble((x:Double) => if (!isNaN(x)) s = s + x)
          s
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var sum = 0.0
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.getDouble(col,row)
                  if(!isNaN(z)) { sum = sum + z }
                }
              }
            )
          }

          sum
      }
    }.reduce(_+_)

  def zonalMin[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var min = Int.MaxValue
          r.foreach((x:Int) => if (x != NODATA && x < min) { min = x })
          min
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var min = Int.MaxValue
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (z != NODATA && z < min) { min = z }
                }
              }
            )
          }

          min
      }
    }.reduce(math.min(_,_))

  def zonalMinDouble[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var min = Double.NaN
          r.foreach((x:Int) => if (!isNaN(x) && (x < min || isNaN(min))) { min = x })
          min
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var min = Double.NaN
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (!isNaN(z) && (z < min || isNaN(min))) { min = z }
                }
              }
            )
          }

          min
      }
    }.reduce(math.min(_,_))

  def zonalMax[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var max = Int.MinValue
          r.foreach((x:Int) => if (x != NODATA && x > max) { max = x })
          max
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var max = Int.MaxValue
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (z != NODATA && z > max) { max = z }
                }
              }
            )
          }
          max
      }
    }.reduce(math.max(_,_))

  def zonalMaxDouble[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var max = Double.NaN
          r.foreach((x:Int) => if (!isNaN(x) && (x > max || isNaN(max))) { max = x })
          max
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var max = Double.NaN
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (!isNaN(z) && (z > max || isNaN(max))) { max = z }
                }
              }
            )
          }
          max
      }
    }.reduce(math.max(_,_))

  def zonalMean[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var s = 0L
          var c = 0L
          r.foreach((x:Int) => if (x != NODATA) { s = s + x; c = c + 1 })
          LongMean(s,c)
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var sum: Long = 0L
          var count: Long = 0L
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (z != NODATA) { sum = sum + z; count = count + 1 }
                }
              }
            )
          }

          LongMean(sum,count)
      }
    }.reduce(_+_).map(_.mean)

  def zonalMeanDouble[D](p:Op[feature.Polygon[D]]) = 
    self.mapIntersecting(p) { tileIntersection =>
      tileIntersection match {
        case FullTileIntersection(r:Raster) =>
          var s = 0.0
          var c = 0L
          r.force.foreachDouble((x:Double) => if (!java.lang.Double.isNaN(x)) { s = s + x; c = c + 1 })
          DoubleMean(s,c)
        case PartialTileIntersection(r:Raster,polygons:List[_]) =>
          var sum = 0.0
          var count = 0L
          for(p <- polygons.asInstanceOf[List[Polygon[D]]]) {
            Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
              new Callback[Geometry,D] {
                def apply(col:Int, row:Int, g:Geometry[D]) {
                  val z = r.get(col,row)
                  if (!isNaN(z)) { sum = sum + z; count = count + 1 }
                }
              }
            )
          }
          DoubleMean(sum,count)
      }
    }.reduce(_+_).map(_.mean)
}

case class LongMean(sum: Long, count: Long) {
  def mean = if (count == 0) {
    geotrellis.NODATA
  } else {
    math.round((sum / count).toDouble)
  }
  def +(b: LongMean) = LongMean(sum + b.sum,count + b.count)
}

case class DoubleMean(sum: Double, count: Double) {
  def mean:Double = if (count == 0) {
    Double.NaN
  } else {
    sum/count
  }
  def +(b: DoubleMean) = DoubleMean(sum + b.sum,count + b.count)
}
