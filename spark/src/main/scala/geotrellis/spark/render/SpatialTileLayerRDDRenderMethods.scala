package geotrellis.spark.render

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

abstract class SpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]] extends MethodExtensions[RDD[(SpatialKey, Tile)] with Metadata[M]] {
  /**
    * Renders each tile as a GeoTiff, represented by the bytes of the GeoTiff file.
    */
  def renderGeoTiff(): RDD[(SpatialKey, Array[Byte])] =
    Render.renderGeoTiff(self)
}
