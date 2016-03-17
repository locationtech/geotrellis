package geotrellis.spark

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.io.hadoop._

import org.apache.spark.rdd.RDD


package object render {
  implicit class SpatialTileLayerRDDRenderMethods(rdd: TileLayerRDD[SpatialKey]) {
    /**
     * Renders each tile as a PNG.
     *
     * @param classifier If not defined cells are assumed to be RGBA values
     */
    def renderPng(colorMap: Option[ColorMap] = None): RDD[(SpatialKey, Array[Byte])] = {
      val paintTile = (k: SpatialKey, t: Tile) => colorMap.fold(t.renderPng())(cm => t.renderPng(cm)).bytes
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }

    /**
     * Renders each tile as a JPG.
     *
     * @param colorMap If not defined cells are assumed to be RGB values
     */
    def renderJpg(colorMap: Option[ColorMap] = None): RDD[(SpatialKey, Array[Byte])] = {
      val paintTile = (k: SpatialKey, t: Tile) => colorMap.fold(t.renderJpg())(cm => t.renderJpg(cm)).bytes
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }

    /**
     * Renders each tile as a GeoTiff.
     */
    def renderGeoTiff(): RDD[(SpatialKey, Array[Byte])] = {
      val transform = rdd.metadata.mapTransform
      val crs = rdd.metadata.crs
      val paintTile = (k: SpatialKey, t: Tile) => GeoTiff(t, transform(k), crs).toByteArray
      rdd.map { case (k, t) => (k, paintTile(k,t)) }
    }
  }
}
