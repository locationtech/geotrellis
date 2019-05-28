/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.timeseries

import geotrellis.raster._
import geotrellis.tiling.SpaceTimeKey
import geotrellis.layers.mask.Mask
import geotrellis.spark._
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
