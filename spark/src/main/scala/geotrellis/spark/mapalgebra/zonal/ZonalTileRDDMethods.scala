package geotrellis.spark.mapalgebra.zonal

import geotrellis.raster.mapalgebra.zonal._
import geotrellis.raster.histogram._
import geotrellis.raster._

import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import org.apache.spark.Partitioner
import org.apache.spark.rdd._

trait ZonalTileRDDMethods[K] extends TileRDDMethods[K] {
  def zonalHistogram(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Int]] =
    Zonal.histogram(self, zonesTileRdd, partitioner)

  def zonalHistogramDouble(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Double]] =
    Zonal.histogramDouble(self, zonesTileRdd, partitioner)

  def zonalPercentage(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    Zonal.percentage(self, zonesTileRdd, partitioner)
}
