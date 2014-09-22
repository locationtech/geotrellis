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
      case TmsTile(t, r) => (t, ZonalHistogram(r, zones))
    }

  def zonalPercentage(zones: Tile): RasterRDD = rasterRDD.mapTiles {
    case TmsTile(t, r) => TmsTile(t, ZonalPercentage(r, zones))
  }
}
