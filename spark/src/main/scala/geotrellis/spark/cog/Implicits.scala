package geotrellis.spark.cog

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.tiling._

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
        overviews = overviews.map(_.asInstanceOf[SinglebandGeoTiff])
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
        overviews.map(_.asInstanceOf[MultibandGeoTiff])
      )
    }
  }
}
