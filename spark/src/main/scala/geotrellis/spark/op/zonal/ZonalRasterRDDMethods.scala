package geotrellis.spark.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.zonal._
import geotrellis.raster.stats.Histogram

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import org.apache.spark.rdd.RDD

trait ZonalRasterRDDMethods extends RasterRDDMethods {
  def zonalHistogram(zones: Tile): RDD[(Long, Map[Int, Histogram])] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, ZonalHistogram(t, zones))
    }

  def zonalPercentage(zones: Tile): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(r, t) => TmsTile(r, ZonalPercentage(t, zones))
  }
}
