package geotrellis.spark.io.s3.geotiff

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.{Raster, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.util._
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.{Extent, ProjectedExtent}
import java.net.URI

import com.amazonaws.services.s3.AmazonS3URI
import geotrellis.spark.io.geotiff.GeoTiffCollectionLayerReader
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.io.s3.S3Client

case class SinglebandGeoTiffCollectionLayerReader(
  seq: Seq[(ProjectedExtent, URI)],
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

    seq
      .filter { case (projectedExtent, p) =>
        projectedExtent
          .reproject(layoutScheme.crs)
          .intersects(mapTransform(SpatialKey(x, y))) && layerId.name == discriminator(p)
      }
      .map { case (projectedExtent, uri) =>
        val auri = new AmazonS3URI(uri)
        println(s"fetching $uri raster...")
        val raster = GeoTiffReader
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
        println(s"fetched $uri raster...")

        println(s"cropping $uri raster...")
        println(
          s"""
             |raster.extent: ${raster.extent}
             |raster.extent.reproject(layoutScheme.crs, projectedExtent.crs): ${raster.extent.reproject(layoutScheme.crs, projectedExtent.crs)}
             |
             |val craster = raster
             |  .crop(
             |    ${mapTransform(SpatialKey(x, y)).reproject(layoutScheme.crs, projectedExtent.crs)},
             |    ${layout.cellSize}
             |  )
           """.stripMargin)

        // for tests to enure that there are no extra segments in the query which may cause issues
        val ext =
          raster
            .extent
            .intersection(mapTransform(SpatialKey(x, y)).reproject(layoutScheme.crs, projectedExtent.crs))
            .getOrElse(Extent(0, 0, 0, 0))

        val craster = raster
          .crop(
            ext,
            layout.cellSize
          )
        println(s"cropped $uri raster...")

        println(s"reprojecting $uri raster...")
        val rraster = craster.reproject(projectedExtent.crs, layoutScheme.crs)
        println(s"reprojected $uri raster...")

        craster

      }
      .reduce(_ merge _)
  }

  def readAll(layerId: LayerId): Seq[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    seq
      .filter { case (_, p) => layerId.name == discriminator(p) }
      .map { case (projectedExtent, uri) =>
        println(s"fetching $uri...")
        val auri = new AmazonS3URI(uri)
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
          .crop(projectedExtent.extent, layout.cellSize)
          .reproject(projectedExtent.crs, layoutScheme.crs)
      }
  }
}

object SinglebandGeoTiffCollectionLayerReader {
  def fetchSingleband(
    paths: Seq[URI],
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ): SinglebandGeoTiffCollectionLayerReader = {
    val s3Client = getS3Client()
    val s3Paths = paths.map(p => new AmazonS3URI(p))

    val seq =
      s3Paths.flatMap { p =>
        s3Client
          .listKeys(p.getBucket, p.getKey)
          .filter { s =>
            // TODO: remove it, required for demo
            s.endsWith(".TIF") && (s.contains("B4") || s.contains("B3") || s.contains("B2"))
          }
          .map(l => new AmazonS3URI(s"s3://${p.getBucket}/$l"))
      }
      .map { auri =>
        val tiff = GeoTiffReader
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

        ProjectedExtent(tiff.extent, tiff.crs) -> new URI(auri.toString)
      }

    SinglebandGeoTiffCollectionLayerReader(seq, layoutScheme, discriminator, getS3Client)
  }
}
