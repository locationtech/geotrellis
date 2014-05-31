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

object LoadRasterUrl {
  def apply(url: Op[String]): LoadRasterUrl = LoadRasterUrl(url, None)
}

/**
 * Load the raster from JSON metadata recieved from a URL
 */
case class LoadRasterUrl(url: Op[String], re: Op[Option[RasterExtent]]) extends Operation[Tile] {
  def _run() = runAsync(List(url, re))
  val nextSteps: Steps = {
    case (url: String) :: (re: Option[_]) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader
          .getRasterLayerFromUrl(url)
          .getRaster(re.asInstanceOf[Option[RasterExtent]])
      }
  }
}
