package geotrellis.spark.timeseries

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mask._
import geotrellis.util.annotations.experimental
import geotrellis.vector._

import org.apache.log4j.Logger
import org.apache.spark.rdd._

import scala.reflect.ClassTag

import java.time.ZonedDateTime


/**
  * Given a TileLayerRDD[SpaceTimeKey], some masking geometry, and a
  * reduction operator, produce a time series.
  *
  * @author James McClain
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object TimeSeries {

  @experimental def apply[R: ClassTag](
    layer: TileLayerRDD[SpaceTimeKey],
    projection: Tile => R,
    reduction: (R, R) => R,
    geoms: Traversable[MultiPolygon],
    options: Mask.Options = Mask.Options.DEFAULT
  ): RDD[(ZonedDateTime, R)] = {

    layer
      .mask(geoms, options)
      .map({ case (key: SpaceTimeKey, tile: Tile) => (key.time, projection(tile)) })
      .reduceByKey(reduction)
  }

}
