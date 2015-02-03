package geotrellis.spark.op.global

import geotrellis.spark._

import geotrellis.raster.op.global.{VerticalFlip => VerticalTileFlip}

import reflect.ClassTag

object VerticalFlip {

  def apply[K](rasterRDD: RasterRDD[K])
    (implicit keyClassTag: ClassTag[K]): RasterRDD[K] = {
    val gridBounds = rasterRDD.metaData.gridBounds
    val rowMax = gridBounds.height - 1

    asRasterRDD(rasterRDD.metaData) {
      rasterRDD
        .mapTiles { tile =>
        VerticalTileFlip(tile)
      }
        .groupBy { case (key, tile) =>
          val SpatialKey(col, row) = key
          val flippedRow = rowMax - row - 1
          if (row > flippedRow) (col, flippedRow)
          else (col, row)
      }
        .flatMap { case ((c, r), seq) =>
          seq match {
            case Seq(first) => seq
            case Seq((firstKey, firstTile), (secondKey, secondTile)) =>
              Seq(
                (firstKey, secondTile),
                (secondKey, firstTile)
              )
          }
      }
    }
  }
}
