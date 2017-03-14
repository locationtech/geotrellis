/*
 * Copyright 2016 Azavea
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

package geotrellis.doc.examples.spark

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.TestEnvironment

import org.joda.time._
import org.scalatest._

class SparkExamplesTests extends FunSuite with Matchers with TestEnvironment with TileLayerRDDBuilders with TileBuilders {
  implicit def longToTemporalKey(l: Long) = TemporalKey(l)

  test("Applying a threshold and then median filter on multiband imagery in an RDD layer") {
    import geotrellis.raster.mapalgebra.focal.Square

    val tiles =
      Seq(
        SpaceTimeKey(SpatialKey(0, 0), 0L) -> MultibandTile(createValueTile(10, 10, 1), createValueTile(10, 10, 2)),
        SpaceTimeKey(SpatialKey(1, 0), 0L) -> MultibandTile(createValueTile(10, 10, 3), createValueTile(10, 10, 4)),
        SpaceTimeKey(SpatialKey(0, 1), 0L) -> MultibandTile(createValueTile(10, 10, 5), createValueTile(10, 10, 6)),
        SpaceTimeKey(SpatialKey(1, 1), 0L) -> MultibandTile(createValueTile(10, 10, 7), createValueTile(10, 10, 8)),

        SpaceTimeKey(SpatialKey(0, 0), 1L) -> MultibandTile(createValueTile(10, 10, 9), createValueTile(10, 10, 10)),
        SpaceTimeKey(SpatialKey(1, 0), 1L) -> MultibandTile(createValueTile(10, 10, 11), createValueTile(10, 10, 12)),
        SpaceTimeKey(SpatialKey(0, 1), 1L) -> MultibandTile(createValueTile(10, 10, 13), createValueTile(10, 10, 14)),
        SpaceTimeKey(SpatialKey(1, 1), 1L) -> MultibandTile(createValueTile(10, 10, 15), createValueTile(10, 10, 16))
      )

    // Using threshold of 15, focalMax instead.

    val rdd = sc.parallelize(tiles)
    val neighborhood = Square(2)
    val result =
      rdd.mapValues { tile =>
        tile.map { (band, z) =>
          if(z > 15) NODATA
          else z
        }
      }
      .bufferTiles(neighborhood.extent)
      .mapValues { bufferedTile =>
        bufferedTile.tile.mapBands { (_, band) =>
          band.focalMax(neighborhood, Some(bufferedTile.targetArea))
        }
      }
      .collect
      .toMap

    // Check some values
    val t1 = result(SpaceTimeKey(SpatialKey(1, 0), 0L))
    t1.band(0).get(5, 8) should be (7)
    t1.band(0).get(5, 7) should be (3)

    val t2 = result(SpaceTimeKey(SpatialKey(1, 1), 1L))
    t2.band(1).get(0, 0) should be (14)
    t2.band(1).get(2, 2) should be (NODATA)
  }
}
