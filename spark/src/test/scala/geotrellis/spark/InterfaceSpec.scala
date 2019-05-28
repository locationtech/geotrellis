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

package geotrellis.spark

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.layers.{LayerId, Metadata, TileLayerMetadata}
import geotrellis.layers.avro.codecs._
import geotrellis.layers.json._
import geotrellis.spark.io._

import org.apache.spark._
import org.apache.spark.rdd._

/**
 * This file provides a specification for raster RDD manipulations we expect to be valid.
 * Its only purprose is to verify that they compile, therefore key implemintations are undefined.
 */
object InterfaceSpec {
  type RRDD = RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]]

  def onlyCompile = {
    val reader: FilteringLayerReader[LayerId] = ???
    val id: LayerId = ???

    val rdd1 = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id)
    val rdd2 = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id)
    val rdd3 = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id)


     rdd1 + 1
     rdd1 localAdd 1
     rdd1 + rdd2
     rdd1 localAdd rdd2
     Seq(rdd1, rdd2, rdd3).localAdd


    import geotrellis.raster.mapalgebra.local._

    // we can break it down and be explicit
    rdd1.join(rdd2).mapValues { case (tile1, tile2) => Add(tile1, tile2) }
    // we can make that operation shorter
    rdd1.join(rdd2).combineValues(Add(_, _))

    // we can update an RDD with a left join
    rdd1.leftOuterJoin(rdd2).mapValues { case (tile1, optionTile) => optionTile.fold(tile1)(Add(tile1, _)) }
    // again we can make that shorter
    rdd1.leftOuterJoin(rdd2).updateValues(Add(_, _))

    // spatial joins behave like joins but do smarter record grouping
    rdd1.spatialJoin(rdd2).combineValues(Add(_, _))
    rdd1.spatialLeftOuterJoin(rdd2).updateValues(Add(_, _))


    // We can preserve metadata explicitly
    ContextRDD(rdd1 localAdd rdd2, rdd1.metadata)

    // Using .withContext is easier
    rdd1.withContext { _ localAdd rdd2 }

    rdd1.withContext { rdd =>
      rdd localAdd rdd2 localAdd rdd3
    }

    // Sometimes you need to chain updating Metadata to operation that changes RDD meaning
    rdd1
      .withContext { _ localEqual 123 }
      .mapContext { metadata => metadata.copy(cellType = BitCellType) }


    // Chaining updates on results of spatialJoin is different because they produce Metadata
    def updateLayout(md: TileLayerMetadata[SpatialKey], bounds: Bounds[SpatialKey]): TileLayerMetadata[SpatialKey] = ???

    rdd1.spatialJoin(rdd2)
      .withContext { _.combineValues(Add(_, _)) }
      .mapContext { bounds => updateLayout(rdd1.metadata, bounds) }

    rdd1 // spatial join will consume and transform my metadata to keybounds
      .spatialJoin(rdd2).withContext { _.combineValues(Add(_, _)) }
      .spatialJoin(rdd3).withContext { _.combineValues(Add(_, _)) } // this saved KeyBounds of the join through combine
      .mapContext{ bounds => updateLayout(rdd1.metadata, bounds) }
  }
}
