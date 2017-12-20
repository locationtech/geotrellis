package geotrellis.spark.io.cog

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.vector.Extent
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

import java.net.URI

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandGeoTiffConstructMethods(val self: Tile) extends GeoTiffConstructMethods[Tile] {
    def toGeoTiff[K](
      nextLayout: LayoutDefinition,
      gb: GridBounds,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[Tile]] = Nil
    ): SinglebandGeoTiff = {
      SinglebandGeoTiff(
        GeoTiffTile(self.crop(nextLayout.mapTransform(gb), md.extent, Crop.Options(clamp = false)), options),
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews
      )
    }
  }

  implicit class withMultibandGeoTiffConstructMethods(val self: MultibandTile) extends GeoTiffConstructMethods[MultibandTile] {
    def toGeoTiff[K](
      nextLayout: LayoutDefinition,
      gb: GridBounds,
      md: TileLayerMetadata[K],
      options: GeoTiffOptions,
      overviews: List[GeoTiff[MultibandTile]] = Nil
    ): MultibandGeoTiff = {
      MultibandGeoTiff(
        GeoTiffMultibandTile(self.crop(nextLayout.mapTransform(gb), md.extent, Crop.Options(clamp = false)), options),
        md.extent,
        md.crs,
        Tags.empty,
        options = options,
        overviews = overviews
      )
    }
  }

  implicit class withSinglebandGeoTiffSegmentConstructMethods[K](val self: Iterable[(K, Tile)]) extends GeoTiffSegmentConstructMethods[K, Tile] {
    def toGeoTiff(
      layout: LayoutDefinition,
      extent: Extent,
      crs: CRS,
      options: GeoTiffOptions,
      tags: Tags = Tags.empty,
      overviews: List[GeoTiff[Tile]] = Nil
    )(implicit sc: SpatialComponent[K]): SinglebandGeoTiff = {
      val gridBounds =
        layout.mapTransform.extentToBounds(extent)

      val tileLayout =
        TileLayout(gridBounds.width, gridBounds.height, layout.tileCols, layout.tileRows)

      val tile =
        GeoTiffTile(
          self.map { case (SpatialKey(col, row), tile) =>
            ((col - gridBounds.colMin, row - gridBounds.rowMin), tile)
          }.toMap,
          tileLayout,
          options
        )

      SinglebandGeoTiff(
        tile,
        extent,
        crs,
        tags,
        options = options,
        overviews = overviews
      )
    }
  }

  implicit class withMultibandGeoTiffSegmentConstructMethods[K](val self: Iterable[(K, MultibandTile)]) extends GeoTiffSegmentConstructMethods[K, MultibandTile] {
    def toGeoTiff(
      layout: LayoutDefinition,
      extent: Extent,
      crs: CRS,
      options: GeoTiffOptions,
      tags: Tags = Tags.empty,
      overviews: List[GeoTiff[MultibandTile]] = Nil
    )(implicit sc: SpatialComponent[K]): MultibandGeoTiff = {
      val gridBounds =
        layout.mapTransform.extentToBounds(extent)

      val tileLayout =
        TileLayout(gridBounds.width, gridBounds.height, layout.tileCols, layout.tileRows)

      val tile =
        GeoTiffMultibandTile(
          self.map { case (SpatialKey(col, row), tile) =>
            ((col - gridBounds.colMin, row - gridBounds.rowMin), tile)
          }.toMap,
          tileLayout,
          options
        )

      MultibandGeoTiff(
        tile,
        extent,
        crs,
        tags,
        options = options,
        overviews = overviews
      )
    }
  }

  implicit class withCOGLayerWriteMethods[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](val self: RDD[(K, GeoTiff[V])]) extends MethodExtensions[RDD[(K, GeoTiff[V])]] {
    def write(keyIndex: KeyIndex[K], uri: URI): Unit =
      COGLayer.write[K, V](self)(keyIndex, uri)
  }
}
