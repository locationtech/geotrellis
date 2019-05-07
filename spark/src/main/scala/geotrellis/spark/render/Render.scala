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
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import org.apache.spark.rdd.RDD

object Render {
  /**
    * Renders each tile as a PNG.
    *
    * @param  rdd   The RDD of spatial tiles to render.
    */
  def renderPng(rdd: RDD[(SpatialKey, Tile)]): RDD[(SpatialKey, Png)] =
    rdd.mapValues(_.renderPng())

  /**
    * Renders each tile as a PNG.
    *
    * @param  rdd   The RDD of spatial tiles to render.
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderPng(rdd: RDD[(SpatialKey, Tile)], colorMap: ColorMap): RDD[(SpatialKey, Png)] =
    rdd.mapValues(_.renderPng(colorMap))

  /**
    * Renders each tile as a JPG. Assumes tiles are already colors of RGBA values.
    *
    * @param  rdd   The RDD of spatial tiles to render.
    */
  def renderJpg(rdd: RDD[(SpatialKey, Tile)]): RDD[(SpatialKey, Jpg)] =
    rdd.mapValues(_.renderJpg())

  /**
    * Renders each tile as a JPG.
    *
    * @param  rdd   The RDD of spatial tiles to render.
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderJpg(rdd: RDD[(SpatialKey, Tile)], colorMap: ColorMap): RDD[(SpatialKey, Jpg)] =
    rdd.mapValues(_.renderJpg(colorMap))

  /**
    * Renders each tile as a SinglebandGeoTiff.
    *
    * @param  rdd   The RDD of spatial tiles to render.
    */
  def renderGeoTiff[
    M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(SpatialKey, Tile)] with Metadata[M]): RDD[(SpatialKey, SinglebandGeoTiff)] =
    rdd.mapPartitions({ partition =>
      val transform = rdd.metadata.getComponent[LayoutDefinition].mapTransform
      val crs = rdd.metadata.getComponent[CRS]
      partition.map { case (key, tile) =>
        (key, GeoTiff(tile, transform(key), crs))
      }
    }, preservesPartitioning = true)

  /**
    * Renders each multiband tile as a MultibandGeoTiff
    *
    * @param  rdd   The RDD of spatial multiband tiles to render.
    */
  def renderGeoTiff[
    M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(SpatialKey, MultibandTile)] with Metadata[M])(implicit d: DummyImplicit): RDD[(SpatialKey, MultibandGeoTiff)] =
    rdd.mapPartitions({ partition =>
      val transform = rdd.metadata.getComponent[LayoutDefinition].mapTransform
      val crs = rdd.metadata.getComponent[CRS]
      partition.map { case (key, tile) =>
        (key, GeoTiff(tile, transform(key), crs))
      }
    }, preservesPartitioning = true)
}
