package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

package object render {
  implicit class withSpatialTileRDDRenderMethods(val self: RDD[(SpatialKey, Tile)])
      extends SpatialTileRDDRenderMethods

  implicit class withSpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]](val self: RDD[(SpatialKey, Tile)] with Metadata[M])
      extends SpatialTileLayerRDDRenderMethods[M]
}
