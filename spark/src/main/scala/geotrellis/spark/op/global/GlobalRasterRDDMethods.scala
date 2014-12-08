package geotrellis.spark.op.global

import geotrellis.spark._
import geotrellis.raster.op.global.VerticalFlip

trait GlobalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  val _sc: SpatialComponent[K]

  def verticalFlip: RasterRDD[K] = {
    val gridBounds = rasterRDD.metaData.gridBounds
    val rowMax = gridBounds.height - 1


    val resRDD = rasterRDD.mapTiles {
      case(key, tile) => (key, VerticalFlip(tile))
    }.groupBy {
      case(key, tile) => {
        val SpatialKey(col, row) = key
        val flippedRow = rowMax - row - 1
        if (row > flippedRow) (col, flippedRow)
        else (col, row)
      }
    }.flatMap {
      case((c, r), seq) => seq match {
        case Seq(first) => seq
        case Seq((firstKey, firstTile), (secondKey, secondTile)) => Seq(
          (firstKey, secondTile),
          (secondKey, firstTile)
        )
      }
    }

    new RasterRDD(resRDD, rasterRDD.metaData)
  }
}
