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

import geotrellis.layers.Metadata
import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.tiling.{LayoutDefinition, SpatialKey}
import geotrellis.util._
import org.apache.spark.rdd.RDD

package object render {
  implicit class withSpatialTileRDDRenderMethods(val self: RDD[(SpatialKey, Tile)])
      extends SpatialTileRDDRenderMethods

  implicit class withSpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]](val self: RDD[(SpatialKey, Tile)] with Metadata[M])
      extends SpatialTileLayerRDDRenderMethods[M]
}
