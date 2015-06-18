package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.local.Minority

trait MinorityRasterRDDMethods[K] extends RasterRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Minority(tiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: RasterRDD[K, Tile]*)(implicit d: DI): RasterRDD[K, Tile] =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Seq[RasterRDD[K, Tile]]): RasterRDD[K, Tile] =
    rasterRDD.combinePairs(others.toSeq) {
      case tiles =>
        (tiles.head.id, Minority(n, tiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: RasterRDD[K, Tile]*)(implicit d: DI): RasterRDD[K, Tile] =
    localMinority(n, rs)
}
