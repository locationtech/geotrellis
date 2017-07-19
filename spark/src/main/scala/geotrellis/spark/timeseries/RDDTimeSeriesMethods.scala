package geotrellis.spark.timeseries

import geotrellis.raster._
import geotrellis.raster.histogram._
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
}

abstract class RDDTimeSeriesMethods
    extends MethodExtensions[TileLayerRDD[SpaceTimeKey]] {

  def polygonalHistogram(
    polygon: Geometry,
    options: Options = Options.DEFAULT
  ): Map[ZonedDateTime, Histogram[Double]] =
    polygonalHistogram(List(polygon), options)

  def polygonalHistogram(
    polygons: Traversable[Geometry]
  ): Map[ZonedDateTime, Histogram[Double]] =
    polygonalHistogram(polygons, Options.DEFAULT)

  def polygonalHistogram(
    polygons: Traversable[Geometry],
    options: Options
  ): Map[ZonedDateTime, Histogram[Double]] = {
    TimeSeries(
      self,
      RDDTimeSeriesFunctions.histogramProjection,
      RDDTimeSeriesFunctions.histogramReduction,
      polygons.map({ p =>
        p match  {
          case p: Polygon => MultiPolygon(p)
          case p: MultiPolygon => p
          case _ => throw new Exception("Polygons and MultiPolygons only, please.")
        }
      }),
      options
    ).collect().toMap
  }

}
