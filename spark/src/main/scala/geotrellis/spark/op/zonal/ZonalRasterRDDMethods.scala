package geotrellis.spark.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.zonal._
import geotrellis.raster.stats.Histogram

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

trait ZonalRasterRDDMethods extends RasterRDDMethods {
  def zonalHistogram(zones: Tile): Seq[Map[Int, Histogram]] =
    rasterRDD
      .collect
      .map(tmsTile => ZonalHistogram(tmsTile.tile, zones))

  def zonalPercentage(zones: Tile): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, ZonalPercentage(t, zones))
  }
}
