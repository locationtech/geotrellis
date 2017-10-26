package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.spark.io.hadoop.HdfsRangeReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

/** Approach with TiffTags stored in a DB */
case class HadoopGeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  conf: Configuration
) {

  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform
    val keyExtent: Extent = mapTransform(SpatialKey(x, y))

    // the question is here, in what CRS to store metadata
    // how to persist it? ~geohash?
    attributeStore
      .query(layerId.name, ProjectedExtent(keyExtent, layoutScheme.crs))
      .map { md =>
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(HdfsRangeReader(new Path(md.uri), conf)),
              false,
              true
            )

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
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(HdfsRangeReader(new Path(md.uri), conf)),
              false,
              true
            )

        tiff
          .crop(tiff.extent, layout.cellSize)
          .reproject(tiff.crs, layoutScheme.crs)
          .resample(layoutScheme.tileSize, layoutScheme.tileSize)
      }
  }
}
