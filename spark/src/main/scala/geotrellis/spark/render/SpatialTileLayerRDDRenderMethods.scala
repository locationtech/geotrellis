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

package geotrellis.spark.render

import geotrellis.layers.Metadata
import geotrellis.proj4.CRS
import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import org.apache.spark.rdd.RDD

abstract class SpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]] extends MethodExtensions[RDD[(SpatialKey, Tile)] with Metadata[M]] {
  /**
    * Renders each tile as a GeoTiff, represented by the bytes of the GeoTiff file.
    */
  def renderGeoTiff(): RDD[(SpatialKey, SinglebandGeoTiff)] =
    Render.renderGeoTiff(self)
}

abstract class SpatialMultiBandTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]] extends MethodExtensions[RDD[(SpatialKey, MultibandTile)] with Metadata[M]] {
  /**
    * Renders each tile as a GeoTiff, represented by the bytes of the GeoTiff file.
    */
  def renderGeoTiff(): RDD[(SpatialKey, MultibandGeoTiff)] =
    Render.renderGeoTiff(self)
}
