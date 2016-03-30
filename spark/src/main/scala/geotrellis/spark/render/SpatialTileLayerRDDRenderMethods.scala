package geotrellis.spark.render

import geotrellis.proj4.CRS
import geotrellis.raster.{Tile, MultibandTile}
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

abstract class SpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]] extends MethodExtensions[RDD[(SpatialKey, Tile)] with Metadata[M]] {
  /**
    * Renders each tile as a GeoTiff, represented by the bytes of the GeoTiff file.
    */
  def renderGeoTiff(): RDD[(SpatialKey, SinglebandGeoTiff)] =
    Render.renderGeoTiff(self)
}

abstract class SpatialMultiBandTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]] extends MethodExtensions[RDD[(SpatialKey, MultibandTile)] with Metadata[M]] {
  /**
    * Renders each tile as a GeoTiff, represented by the bytes of the GeoTiff file.
    */
  def renderGeoTiff(): RDD[(SpatialKey, MultibandGeoTiff)] =
    Render.renderGeoTiff(self)
}
