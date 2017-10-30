package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.vector.{Extent, ProjectedExtent}

import java.net.URI

/** Approach with TiffTags stored in a DB */
trait GeoTiffLayerReader[M[T] <: Traversable[T]] {
  val attributeStore: AttributeStore[M, GeoTiffMetadata]
  val layoutScheme: ZoomedLayoutScheme

  protected def readSingleband(uri: URI): SinglebandGeoTiff

  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform
    val keyExtent: Extent = mapTransform(SpatialKey(x, y))

    attributeStore
      .query(layerId.name, ProjectedExtent(keyExtent, layoutScheme.crs))
      .map { md =>
        val tiff = readSingleband(md.uri)
        val reprojectedKeyExtent = keyExtent.reproject(layoutScheme.crs, tiff.crs)

        val ext =
          tiff
            .extent
            .intersection(reprojectedKeyExtent)
            .getOrElse(reprojectedKeyExtent)

          tiff
            .crop(ext, layout.cellSize)
            .reproject(tiff.crs, layoutScheme.crs)
            .resample(RasterExtent(keyExtent, layoutScheme.tileSize, layoutScheme.tileSize))
      }
      .reduce(_ merge _)
  }

  def readAll(layerId: LayerId): Traversable[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    attributeStore
      .query(layerId.name)
      .map { md =>
        val tiff = readSingleband(md.uri)
        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      }
  }
}
