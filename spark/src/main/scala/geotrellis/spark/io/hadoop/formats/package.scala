/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._

import org.apache.hadoop.io.Writable

package object formats {
//  type WritableTile = (SpatialKeyWritable, TileWritable)

  // implicit class ToWritableWrapper(idTile: (SpatialKey, Tile)) {
  //   def toWritable(): WritableTile =
  //     (SpatialKeyWritable(idTile.id), TileWritable.fromTile(idTile.tile))
  // }

  // implicit class WritableTileWrapper(wt: WritableTile) {
  //   def toIdTile(metaData: RasterMetaData): (Long, Tile) = {
  //     val tileId = 
  //       wt._1.get
  //     val tile = 
  //       wt._2.toTile(metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)

  //     (tileId, tile)
  //   }

  //   def toTuple(metaData: RasterMetaData): (SpatialKey, Tile) =
  //     wt._1.get -> wt._2.toTile(metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)
  // }
}
