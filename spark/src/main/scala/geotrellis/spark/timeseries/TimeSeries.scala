package geotrellis.spark.timeseries

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mask._
import geotrellis.vector._

import org.apache.log4j.Logger
import org.apache.spark.rdd._

import scala.reflect.ClassTag

import java.time.{ZoneOffset, ZonedDateTime}


/**
  * Given a TileLayerRDD[SpaceTimeKey], some masking geometry, and a
  * reduction operator, produce a time series.
  *
  * @author James McClain
  */
object TimeSeries {

  val logger = Logger.getLogger(TimeSeries.getClass)

  def apply[R: ClassTag](
    layer: TileLayerRDD[SpaceTimeKey],
    projection: Tile => R,
    reduction: (R, R) => R,
    geoms: Traversable[MultiPolygon],
    options: Mask.Options = Mask.Options.DEFAULT
  ): RDD[(ZonedDateTime, R)] = {

    layer
      .mask(geoms, options)
      .map({ case (key: SpaceTimeKey, tile: Tile) =>
        val time = ZonedDateTime.ofInstant(key.instant, ZoneOffset.UTC)
        val data: R = projection(tile)
        (time, data) })
      .reduceByKey(reduction)
  }

}
