package geotrellis.spark.io.s3.geotiff

import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.{ArrayTile, CellType, IntArrayTile, Raster, RasterExtent, Tile}
import geotrellis.raster.stitch._
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.spark.io.hadoop.geotiff.{AttributeStore, GeoTiffMetadata}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.io.s3.S3Client
import com.amazonaws.services.s3.AmazonS3URI


/** Approach with TiffTags stored in a DB */
case class S3GeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  getS3Client: () => S3Client = () => S3Client.DEFAULT
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
        val auri = new AmazonS3URI(md.uri)
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(
                S3RangeReader(
                  bucket = auri.getBucket,
                  key = auri.getKey,
                  client = getS3Client()
                )
              ),
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
        val auri = new AmazonS3URI(md.uri)
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(
                S3RangeReader(
                  bucket = auri.getBucket,
                  key = auri.getKey,
                  client = getS3Client()
                )
              ),
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
