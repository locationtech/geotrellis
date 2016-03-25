package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.tiling.LayoutDefinition

import org.apache.spark.rdd.RDD


package object render {
  implicit class SpatialTileRDDRenderMethods(val rdd: RDD[(SpatialKey, Tile)]) {
    def color(colorMap: ColorMap): RDD[(SpatialKey, Tile)] =
      rdd.mapValues(_.color(colorMap))

    /**
     * Renders each tile as a PNG.
     *
     * @param classifier If not defined cells are assumed to be RGBA values
     */
    def renderPng(colorMap: Option[ColorMap] = None): RDD[(SpatialKey, Png)] = {
      val paintTile = (k: SpatialKey, t: Tile) => colorMap.fold(t.renderPng())(cm => t.renderPng(cm))
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }

    /**
     * Renders each tile as a JPG.
     *
     * @param colorMap If not defined cells are assumed to be RGB values
     */
    def renderJpg(colorMap: Option[ColorMap] = None): RDD[(SpatialKey, Jpg)] = {
      val paintTile = (k: SpatialKey, t: Tile) => colorMap.fold(t.renderJpg())(cm => t.renderJpg(cm))
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }
  }

  implicit class SpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]](val rdd: RDD[(SpatialKey, Tile)] with Metadata[M]) {
    /**
     * Renders each tile as a GeoTiff.
     */
    def renderGeoTiff(): RDD[(SpatialKey, Array[Byte])] = {
      val transform = rdd.metadata.getComponent[LayoutDefinition]mapTransform
      val crs = rdd.metadata.getComponent[CRS]
      val paintTile = (k: SpatialKey, t: Tile) => GeoTiff(t, transform(k), crs).toByteArray
      rdd.map { case (k, t) => (k, paintTile(k,t)) }
    }
  }
}
