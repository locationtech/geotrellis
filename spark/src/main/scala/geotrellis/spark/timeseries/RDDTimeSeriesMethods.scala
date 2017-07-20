package geotrellis.spark.timeseries

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.summary.polygonal._
import geotrellis.spark._
import geotrellis.spark.mask.Mask.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector._

import java.time.ZonedDateTime


object RDDTimeSeriesFunctions {

  def histogramProjection(tile: Tile): StreamingHistogram =
    StreamingHistogram.fromTile(tile)

  def histogramReduction(left: StreamingHistogram, right: StreamingHistogram): StreamingHistogram =
    left + right

  def meanReduction(left: MeanResult, right: MeanResult): MeanResult =
    left + right

  def maxReduction(left: Double, right: Double): Double =
    scala.math.max(left, right)

  def minReduction(left: Double, right: Double): Double =
    scala.math.min(left, right)

  def sumReduction(left: Double, right: Double): Double =
    left + right
}

abstract class RDDTimeSeriesMethods
    extends MethodExtensions[TileLayerRDD[SpaceTimeKey]] {

  private def geometriesToMultiPolygons(
    polygons: Traversable[Geometry]
  ): Traversable[MultiPolygon] = {
    polygons.map({ p =>
      p match  {
        case p: Polygon => MultiPolygon(p)
        case p: MultiPolygon => p
        case p: GeometryCollection => {
          if (p.polygons.length > 0) MultiPolygon(p.polygons.head)
          else if (p.multiPolygons.length > 0) p.multiPolygons.head
          else throw new Exception(s"Cannot handle $p.")
        }
      }
    })
  }

  def sumSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    sumSeries(List(polygon), options)

  def sumSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    sumSeries(List(polygon), Options.DEFAULT)

  def sumSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    sumSeries(polygons, Options.DEFAULT)

  def sumSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      SumDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.sumReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  def minSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    minSeries(List(polygon), options)

  def minSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    minSeries(List(polygon), Options.DEFAULT)

  def minSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    minSeries(polygons, Options.DEFAULT)

  def minSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MinDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.minReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  def maxSeries(
    polygon: MultiPolygon,
    options: Options
  ): Map[ZonedDateTime, Double] =
    maxSeries(List(polygon), options)

  def maxSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    maxSeries(List(polygon), Options.DEFAULT)

  def maxSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    maxSeries(polygons, Options.DEFAULT)

  def maxSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MaxDoubleSummary.handleFullTile,
      RDDTimeSeriesFunctions.maxReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

  def meanSeries(
    polygon: MultiPolygon,
    options: Options = Options.DEFAULT
  ): Map[ZonedDateTime, Double] =
    meanSeries(List(polygon), options)

  def meanSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Double] =
    meanSeries(polygon, Options.DEFAULT)

  def meanSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Double] =
    meanSeries(polygons, Options.DEFAULT)

  def meanSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Double] = {
    TimeSeries(
      self,
      MeanResult.fromFullTileDouble,
      RDDTimeSeriesFunctions.meanReduction,
      polygons,
      options
    )
      .mapValues({ mr => mr.mean })
      .collect()
      .toMap
  }

  def histogramSeries(
    polygon: MultiPolygon,
    options: Options = Options.DEFAULT
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(List(polygon), options)

  def histogramSeries(
    polygon: MultiPolygon
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(polygon, Options.DEFAULT)

  def histogramSeries(
    polygons: Traversable[MultiPolygon]
  ): Map[ZonedDateTime, Histogram[Double]] =
    histogramSeries(polygons, Options.DEFAULT)

  def histogramSeries(
    polygons: Traversable[MultiPolygon],
    options: Options
  ): Map[ZonedDateTime, Histogram[Double]] = {
    TimeSeries(
      self,
      RDDTimeSeriesFunctions.histogramProjection,
      RDDTimeSeriesFunctions.histogramReduction,
      polygons,
      options
    )
      .collect()
      .toMap
  }

}
