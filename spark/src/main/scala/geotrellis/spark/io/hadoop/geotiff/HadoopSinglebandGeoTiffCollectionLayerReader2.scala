package geotrellis.spark.io.hadoop.geotiff

import geotrellis.proj4.{CRS, WebMercator}
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.{CellType, GridBounds, Raster, RasterExtent, SinglebandRaster, Tile}
import geotrellis.spark.tiling.{LayoutDefinition, ZoomedLayoutScheme}
import geotrellis.spark.{Bounds, ContextCollection, EmptyBounds, KeyBounds, LayerId, SpatialKey, TileLayerMetadata}
import geotrellis.util.StreamingByteReader
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.raster.io.geotiff.tags.TiffTags
import java.net.URI

import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

/** Approach with TiffTags stored in a DB */
/* case class HadoopSinglebandGeoTiffCollectionLayerReader2(
  // seq can be stored in some backend
  /** This should be done in a separate interface */
  attributeStore: HadoopGeoTiffAttributeStore,
  layoutScheme: ZoomedLayoutScheme,
  discriminator: (String, String) => Boolean,
  conf: Configuration
) {

  def read(layerId: LayerId)(x: Int, y: Int): Raster[Tile] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mapTransform = layout.mapTransform
    val keyExtent: Extent = mapTransform(SpatialKey(x, y))

    attributeStore
      .get(
        layerName = Some(layerId.name),
        extent = Some(ProjectedExtent(keyExtent, layoutScheme.crs)),
        discriminator = Some(discriminator)
      ).map { md =>
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
      }
      .reduce(_ merge _)
      .resample(RasterExtent(keyExtent, layoutScheme.tileSize, layoutScheme.tileSize))
  }

  def readAll(layerId: LayerId): Seq[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    attributeStore
      .get(
        layerName = Some(layerId.name),
        discriminator = Some(discriminator)
      ).map { md =>
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
    }

  }

  /*def keyedReadAll(layerId: LayerId): Seq[Raster[Tile]] = {
    val layout =
      layoutScheme
        .levelForZoom(layerId.zoom)
        .layout

    val mt = layout.mapTransform

    val result: Seq[(GridBounds, SinglebandRaster)] =
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

          val res =
            tiff
              .crop(tiff.extent, layout.cellSize)
              .reproject(tiff.crs, layoutScheme.crs)

          null
        }

    ContextCollection(result, collectMetadata(layerId.zoom))

    null
  }*/
}

object HadoopSinglebandGeoTiffCollectionLayerReader2 {
  def fetchSingleband(
    path: URI,
    layoutScheme: ZoomedLayoutScheme = ZoomedLayoutScheme(WebMercator),
    discriminator: URI => String = uri => uri.toString.split("/").last.split("\\.").head,
    filterPaths: String => Boolean = _ => true,
    conf: Configuration = new Configuration
  ): HadoopSinglebandGeoTiffCollectionLayerReader2 = {
    val seq =
      HdfsUtils
        .listFiles(new Path(path), conf)
        .map { p =>
          val tiffTags = TiffTagsReader.read(StreamingByteReader(HdfsRangeReader(p, conf)))
          ProjectedExtent(tiffTags.extent, tiffTags.crs) -> p.toUri
        }

    //HadoopSinglebandGeoTiffCollectionLayerReader2(seq, layoutScheme, discriminator, conf)
    null
  }
} */
