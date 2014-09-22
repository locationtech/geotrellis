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
      case TmsTile(t, r) => TmsTile(t, Min(r, n, bounds))
    }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Max(r, n, bounds))
    }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Mode(r, n, bounds))
    }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Median(r, n, bounds))
    }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Mean(r, n, bounds))
    }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Sum(r, n, bounds))
    }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(
    n: Neighborhood,
    bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, StandardDeviation(r, n, bounds))
    }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, Conway(r, Square(1), bounds))
    }

  /**
    * Calculates spatial autocorrelation of cells based on the similarity
    * to neighboring values.
    *
    * @see [[RasterRDDMoransICalculation]]
    */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): RasterRDD =
    rasterRDD.mapTiles {
      case TmsTile(t, r) => TmsTile(t, TileMoransICalculation(r, n, bounds))
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
      case TmsTile(t, r) => (t, ScalarMoransICalculation(r, n, bounds))
    }
}
