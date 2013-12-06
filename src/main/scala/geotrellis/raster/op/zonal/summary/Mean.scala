package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.source._
import geotrellis.feature._
import geotrellis.feature.rasterize._

case class MeanResult(sum: Double, count: Long) {
  def mean:Double = if (count == 0) {
    Double.NaN
  } else {
    sum/count
  }
  def +(b: MeanResult) = MeanResult(sum + b.sum,count + b.count)
}

object MeanResult {
  def fromFullTile(tile:Raster) = {
    var s = 0
    var c = 0L
    tile.foreach((x:Int) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s,c)
  }

  def fromFullTileDouble(tile:Raster) = {
    var s = 0.0
    var c = 0L
    tile.foreachDouble((x:Double) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s,c)
  }
}

object Mean extends TileSummary[MeanResult,Double,ValueSource[Double]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):MeanResult = {
    val PartialTileIntersection(r,polygons) = pt
    var sum = 0.0
    var count = 0L
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.get(col,row)
            if (isData(z)) { sum = sum + z; count = count + 1 }
          }
        }
      )
    }
    MeanResult(sum,count)
  }

  def handleFullTile(ft:FullTileIntersection):MeanResult =
    MeanResult.fromFullTile(ft.tile)

  def converge(ds:DataSource[MeanResult,_]) =
    ds.reduce(_+_).map(_.mean)
}

object MeanDouble extends TileSummary[MeanResult,Double,ValueSource[Double]] {
  def handlePartialTile[D](pt:PartialTileIntersection[D]):MeanResult = {
    val PartialTileIntersection(r,polygons) = pt
    var sum = 0.0
    var count = 0L
    for(p <- polygons) {
      Rasterizer.foreachCellByFeature(p, r.rasterExtent)(
        new Callback[Geometry,D] {
          def apply(col:Int, row:Int, g:Geometry[D]) {
            val z = r.getDouble(col,row)
            if (isData(z)) { sum = sum + z; count = count + 1 }
          }
        }
      )
    }
    MeanResult(sum,count)
  }

  def handleFullTile(ft:FullTileIntersection):MeanResult =
    MeanResult.fromFullTileDouble(ft.tile)

  def converge(ds:DataSource[MeanResult,_]) =
    ds.reduce(_+_).map(_.mean)
}
