package geotrellis.spark.io.s3.geotiff

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.Extent
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.io.s3.S3Client

import com.amazonaws.services.s3.AmazonS3URI

import java.net.URI

/** Approach with TiffTags stored in a DB */
case class S3SinglebandGeoTiffCollectionLayerReader(
  // seq can be stored in some backend
  seq: Seq[(TiffTags, URI)],
  layoutScheme: ZoomedLayoutScheme,
  discriminator: URI => String,
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
    seq
      .filter { case (tiffTags, p) =>
        tiffTags.extent
          .reproject(tiffTags.crs, layoutScheme.crs)
          .intersects(keyExtent) && layerId.name == discriminator(p)
      }
      .map { case (tiffTags, uri) =>
        val auri = new AmazonS3URI(uri)

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
              tiffTags,
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
      }
      .reduce(_ merge _)
      .resample(RasterExtent(keyExtent, layoutScheme.tileSize, layoutScheme.tileSize))
  }

  def readAll(layerId: LayerId): Seq[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    seq
      .filter { case (_, p) => layerId.name == discriminator(p) }
      .map { case (tiffTags, uri) =>
        val auri = new AmazonS3URI(uri)
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
              tiffTags,
              false,
              true
            )

            tiff
              .crop(tiff.extent, layout.cellSize)
              .reproject(tiff.crs, layoutScheme.crs)
      }
  }
}

object S3SinglebandGeoTiffCollectionLayerReader {
  def fetchSingleband(
    paths: Seq[URI],
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head,
    filterPaths: String => Boolean = _ => true,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ): S3SinglebandGeoTiffCollectionLayerReader = {
    val s3Client = getS3Client()
    val s3Paths = paths.map(p => new AmazonS3URI(p))

    val seq =
      s3Paths.flatMap { p =>
        s3Client
          .listKeys(p.getBucket, p.getKey)
          .filter(filterPaths)
          .map(l => new AmazonS3URI(s"s3://${p.getBucket}/$l"))
      }
      .map { auri =>
        val tiffTags = TiffTagsReader.read(StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key = auri.getKey,
            client = getS3Client()
          )
        ))

        tiffTags -> new URI(auri.toString)
      }

    new S3SinglebandGeoTiffCollectionLayerReader(seq, layoutScheme, discriminator, getS3Client)
  }
}
