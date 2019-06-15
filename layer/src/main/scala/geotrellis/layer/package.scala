package geotrellis


import geotrellis.layer.{KeyBounds, MapKeyTransform}
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.util._
import org.locationtech.proj4j.UnsupportedParameterException

import scala.util.{Failure, Success, Try}

package object layer extends Implicits {
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

  implicit class GeoTiffInfoMethods(that: GeoTiffReader.GeoTiffInfo) {
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
