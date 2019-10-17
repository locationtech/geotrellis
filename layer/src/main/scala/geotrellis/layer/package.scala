/*
 * Copyright 2019 Azavea
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

package geotrellis

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffInfo
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.util._

import org.locationtech.proj4j.UnsupportedParameterException

package object layer extends layer.Implicits {
  type TileBounds = GridBounds[Int]
  type SpatialComponent[K] = Component[K, SpatialKey]
  type TemporalComponent[K] = Component[K, TemporalKey]
  type RasterCollection[M] = Seq[Raster[Tile]] with Metadata[M]
  type MultibandRasterCollection[M] = Seq[Raster[MultibandTile]] with Metadata[M]
  type TileLayerCollection[K] = Seq[(K, Tile)] with Metadata[TileLayerMetadata[K]]
  type MultibandTileLayerCollection[K] = Seq[(K, MultibandTile)] with Metadata[TileLayerMetadata[K]]

  object TileLayerCollection {
    def apply[K](seq: Seq[(K, Tile)], metadata: TileLayerMetadata[K]): TileLayerCollection[K] =
      new ContextCollection(seq, metadata)
  }

  object MultibandTileLayerCollection {
    def apply[K](seq: Seq[(K, MultibandTile)], metadata: TileLayerMetadata[K]): MultibandTileLayerCollection[K] =
      new ContextCollection(seq, metadata)
  }

  implicit class GeoTiffInfoMethods(that: GeoTiffInfo) {
    def mapTransform =
      MapKeyTransform(
        extent = that.extent,
        layoutCols = that.segmentLayout.tileLayout.layoutCols,
        layoutRows = that.segmentLayout.tileLayout.layoutRows)
  }

  private final val WORLD_WSG84 = Extent(-180, -89.99999, 179.99999, 89.99999)

  implicit class CRSWorldExtent(val crs: CRS) extends AnyVal {
    def worldExtent: Extent =
      crs match {
        case LatLng =>
          WORLD_WSG84
        case WebMercator =>
          Extent(-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244)
        case Sinusoidal =>
          Extent(-2.0015109355797417E7, -1.0007554677898709E7, 2.0015109355797417E7, 1.0007554677898709E7)
        case c if c.proj4jCrs.getProjection.getName == "utm" =>
          throw new UnsupportedParameterException(
            s"Projection ${c.toProj4String} is not supported as a WorldExtent projection, " +
            s"use a different projection for your purposes or use a different LayoutScheme."
          )
        case _ =>
          WORLD_WSG84.reproject(LatLng, crs)
      }
  }

}
