package geotrellis.spark.mapalgebra.global

import geotrellis.raster.Tile
import geotrellis.spark._

import geotrellis.raster.mapalgebra.global.{VerticalFlip => VerticalTileFlip}
import org.apache.spark.Partitioner

import reflect.ClassTag

object VerticalFlip {
  def apply[K](rasterRDD: RasterRDD[K], partitioner: Option[Partitioner] = None)
              (implicit keyClassTag: ClassTag[K]): RasterRDD[K] = {
    val gridBounds = rasterRDD.metaData.gridBounds
    val rowHeight = gridBounds.height
    def pf: PartialFunction[(K, Tile), (Int, Int)] = { case (key, tile) =>
      val SpatialKey(col, row) = key
      val flippedRow = rowHeight - row - 1
      if (row > flippedRow) (col, flippedRow)
      else (col, row)
    }

    rasterRDD.withContext { rdd =>
      val values = rdd.mapValues { tile => VerticalTileFlip(tile) }
      partitioner
        .fold(values.groupBy(pf))(values.groupBy(pf, _))
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
