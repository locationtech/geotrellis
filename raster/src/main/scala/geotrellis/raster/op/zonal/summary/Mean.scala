package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

case class MeanResult(sum: Double, count: Long) {
  def mean: Double = if (count == 0) {
    Double.NaN
  } else {
    sum/count
  }
  def +(b: MeanResult) = MeanResult(sum + b.sum, count + b.count)
}

object MeanResult {
  def fromFullTile(tile: Tile) = {
    var s = 0
    var c = 0L
    tile.foreach((x: Int) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s, c)
  }

  def fromFullTileDouble(tile: Tile) = {
    var s = 0.0
    var c = 0L
    tile.foreachDouble((x: Double) => if (isData(x)) { s = s + x; c = c + 1 })
    MeanResult(s, c)
  }
}

object Mean extends TileIntersectionHandler[MeanResult, Double]
    with Serializable {
  def handlePartialTile(pt: PartialTileIntersection): MeanResult = {
    val PartialTileIntersection(tile, _, polygon) = pt
    val rasterExtent = pt.rasterExtent
    var sum = 0.0
    var count = 0L
    if(tile.cellType.isFloatingPoint) {
      Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = tile.getDouble(col, row)
            if (isData(z)) { sum = sum + z; count = count + 1 }
          }
        }
      )
    } else {
      Rasterizer.foreachCellByGeometry(polygon, rasterExtent)(
        new Callback {
          def apply(col: Int, row: Int) {
            val z = tile.get(col, row)
            if (isData(z)) { sum = sum + z; count = count + 1 }
          }
        }
      )
    }

    MeanResult(sum, count)
  }

  def handleFullTile(ft: FullTileIntersection): MeanResult =
    if(ft.tile.cellType.isFloatingPoint) {
      MeanResult.fromFullTileDouble(ft.tile)
    } else {
      MeanResult.fromFullTile(ft.tile)
    }

  def combineResults(rs: Seq[MeanResult]): Double =
    rs.foldLeft(MeanResult(0.0, 0L))(_+_).mean
}
