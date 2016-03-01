package geotrellis.spark

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.io.hadoop._

import org.apache.spark.rdd.RDD


package object render {
  implicit class SpatialRasterRDDRenderMethods(rdd: RasterRDD[SpatialKey]) {
    /**
     * Renders each tile as a PNG.
     *
     * @param classifier If not defined cells are assumed to be RGBA values
     */
    def renderPng(classifier: Option[ColorClassifier[_]] = None): RDD[(SpatialKey, Array[Byte])] = {
      val paintTile = (k: SpatialKey, t: Tile) => classifier.fold(t.renderPng())( b => t.renderPng(b)).bytes
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }

    /**
     * Renders each tile as a JPG.
     *
     * @param classifier If not defined cells are assumed to be RGB values
     */
    def renderJpg(classifier: Option[ColorClassifier[_]] = None): RDD[(SpatialKey, Array[Byte])] = {
      val paintTile = (k: SpatialKey, t: Tile) => classifier.fold(t.renderJpg())( b => t.renderJpg(b)).bytes
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }

    /**
     * Renders each tile as a GeoTiff.
     */
    def renderGeoTiff(): RDD[(SpatialKey, Array[Byte])] = {
      val transform = rdd.metaData.mapTransform
      val crs = rdd.metaData.crs
      val paintTile = (k: SpatialKey, t: Tile) => GeoTiff(t, transform(k), crs).toByteArray
      rdd.map { case (k,t) => (k, paintTile(k,t)) }
    }
  }
}
