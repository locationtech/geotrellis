package geotrellis.spark.op.focal

import geotrellis.raster._
import geotrellis.raster.op.focal._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import org.apache.spark.rdd.RDD

trait FocalRasterRDDMethods extends RasterRDDMethods {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Min(t, n, bounds))
    }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Max(t, n, bounds))
    }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Mode(t, n, bounds))
    }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Median(t, n, bounds))
    }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Mean(t, n, bounds))
    }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Sum(t, n, bounds))
    }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(
    n: Neighborhood,
    bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, StandardDeviation(t, n, bounds))
    }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, Conway(t, Square(1), bounds))
    }

  /**
    * Calculates spatial autocorrelation of cells based on the similarity
    * to neighboring values.
    *
    * @see [[RasterRDDMoransICalculation]]
    */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(r, t) => TmsTile(r, TileMoransICalculation(t, n, bounds))
    }

  /**
    * Calculates global spatial autocorrelation of a raster based on the
    * similarity to neighboring values.
    *
    * @see [[ScalarMoransICalculation]]
    */
  def scalarMoransI(
    n: Neighborhood,
    bounds: Option[GridBounds] = None): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, ScalarMoransICalculation(t, n, bounds))
    }
}
