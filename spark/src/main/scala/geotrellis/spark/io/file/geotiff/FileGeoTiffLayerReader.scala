package geotrellis.spark.io.file.geotiff

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.Filesystem
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.spark.io.hadoop.geotiff._

/** Approach with TiffTags stored in a DB */
case class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme
) {

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
        val tiff = GeoTiffReader.readSingleband(Filesystem.toMappedByteBuffer(md.uri.toString), false, true)
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
        val tiff = GeoTiffReader.readSingleband(Filesystem.toMappedByteBuffer(md.uri.toString), false, true)
        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      }
  }
}
