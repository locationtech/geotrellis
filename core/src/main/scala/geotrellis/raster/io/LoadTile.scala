/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.io

import geotrellis.process._

object LoadTile {
  def apply(n: String, col: Op[Int], row: Op[Int]): LoadTile =
    LoadTile(LayerId(n), col, row, None)

  def apply(n: String, col: Int, row: Int, re: RasterExtent): LoadTile =
    LoadTile(LayerId(n), col, row, Some(re))

  def apply(ds: String, n: String, col: Int, row: Int): LoadTile =
    LoadTile(LayerId(ds, n), col, row, None)

  def apply(ds: String, n: String, col: Int, row: Int, re: RasterExtent): LoadTile =
    LoadTile(LayerId(ds, n), col, row, None)

  def apply(layerId: LayerId, col: Int, row: Int): LoadTile =
    LoadTile(layerId, col, row, None)
}

case class LoadTile(layerId: Op[LayerId],
                    col: Op[Int],
                    row: Op[Int],
                    targetExtent: Op[Option[RasterExtent]]) extends Op[Tile] {
  def _run() = runAsync(List(layerId, col, row, targetExtent))
  val nextSteps: Steps = {
    case (layerId: LayerId) :: 
         (col: Int) :: 
         (row: Int) :: 
         (te: Option[_]) :: Nil =>
      LayerResult { layerLoader =>
        val layer = layerLoader.getRasterLayer(layerId)
        layer.getTile(col, row, te.asInstanceOf[Option[RasterExtent]])
      }
  }
}
