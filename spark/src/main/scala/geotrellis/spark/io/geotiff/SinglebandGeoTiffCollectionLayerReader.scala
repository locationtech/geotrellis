package geotrellis.spark.io.geotiff

import geotrellis.proj4.WebMercator
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.{Raster, Tile}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.util._
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.util.Filesystem
import geotrellis.vector.Extent

import java.net.URI
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._

case class SinglebandGeoTiffCollectionLayerReader(
  seq: Seq[(Extent, URI)],
  layoutScheme: ZoomedLayoutScheme,
  discriminator: URI => String
) extends GeoTiffCollectionLayerReader[Tile] {
  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform

    seq
      .filter { case (extent, p) =>
        extent.intersects(mapTransform(SpatialKey(x, y))) && layerId.name == discriminator(p)
      }
      .map { case (_, uri) =>
        GeoTiffReader
          .readSingleband(Filesystem.toMappedByteBuffer(uri.toString), false, true)
          .crop(mapTransform(SpatialKey(x, y)), layout.cellSize)
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
      .map { case (extent, uri) =>
        GeoTiffReader
          .readSingleband(Filesystem.toMappedByteBuffer(uri.toString), false, true)
          .crop(extent, layout.cellSize)
      }
  }
}

object SinglebandGeoTiffCollectionLayerReader {
  def fetchSingleband(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head
  ): GeoTiffCollectionLayerReader[Tile] = {
    val seq =
      Files
        .walk(Paths.get(path))
        .iterator()
        .asScala
        .toList
        .collect { case p if Files.isRegularFile(p) =>
          val uri = new URI(p.toAbsolutePath.toString)
          val tiff = GeoTiffReader.readSingleband(Filesystem.toMappedByteBuffer(uri.toString), false, true)
          tiff.extent -> uri
        }

    SinglebandGeoTiffCollectionLayerReader(seq, layoutScheme, discriminator)
  }
}
