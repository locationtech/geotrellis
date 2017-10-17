package geotrellis.spark.io.s3.geotiff

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.{Raster, RasterExtent, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.Extent
import geotrellis.raster.io.geotiff.tags.TiffTags
import java.net.URI

import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

/** Approach with TiffTags stored in a DB */
case class HadoopSinglebandGeoTiffCollectionLayerReader(
  // seq can be stored in some backend
  seq: Seq[(TiffTags, URI)],
  layoutScheme: ZoomedLayoutScheme,
  discriminator: URI => String,
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
    seq
      .filter { case (tiffTags, p) =>
        tiffTags.extent
          .reproject(tiffTags.crs, layoutScheme.crs)
          .intersects(keyExtent) && layerId.name == discriminator(p)
      }
      .map { case (tiffTags, uri) =>
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(HdfsRangeReader(new Path(uri), conf)),
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
        val tiff =
          GeoTiffReader
            .readSingleband(
              StreamingByteReader(HdfsRangeReader(new Path(uri), conf)),
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

object HadoopSinglebandGeoTiffCollectionLayerReader {
  def fetchSingleband(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head,
    filterPaths: String => Boolean = _ => true,
    conf: Configuration = new Configuration
  ): HadoopSinglebandGeoTiffCollectionLayerReader = {
    val seq =
      HdfsUtils
        .listFiles(new Path(path), conf)
        .map { p =>
          val tiffTags = TiffTagsReader.read(StreamingByteReader(HdfsRangeReader(p, conf)))
          tiffTags -> p.toUri
        }

    HadoopSinglebandGeoTiffCollectionLayerReader(seq, layoutScheme, discriminator, conf)
  }
}
