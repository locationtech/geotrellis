package geotrellis.spark

import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render._
import geotrellis.spark.io.hadoop._

import org.apache.spark.rdd.RDD


package object render {
  sealed case class RenderedImages[K](rdd: RDD[(K, Array[Byte])])

  implicit class SpatialRasterRDDRenderMethods(rdd: RasterRDD[SpatialKey]) {
    /**
     * Renders each tile as a PNG.
     *
     * @param breaks If not defined cells are assumed to be RGBA values
     */
    def renderPng(breaks: Option[ColorBreaks] = None): RenderedImages[SpatialKey] = {
      val paintTile = (k: SpatialKey, t: Tile) => breaks.fold(t.renderPng())( b => t.renderPng(b)).bytes
      RenderedImages(rdd.map { case (k,t) => (k, paintTile(k,t)) })
    }

    /**
     * Renders each tile as a JPG.
     *
     * @param breaks If not defined cells are assumed to be RGB values
     */
    def renderJpg(breaks: Option[ColorBreaks] = None): RenderedImages[SpatialKey] = {
      val paintTile = (k: SpatialKey, t: Tile) => breaks.fold(t.renderJpg())( b => t.renderJpg(b)).bytes
      RenderedImages(rdd.map { case (k,t) => (k, paintTile(k,t)) })
    }

    /**
     * Renders each tile as a GeoTiff.
     */
    def renderGeoTiff(): RenderedImages[SpatialKey] = {
      val transform = rdd.metaData.mapTransform
      val crs = rdd.metaData.crs
      val paintTile = (k: SpatialKey, t: Tile) => GeoTiff(t, transform(k), crs).toByteArray
      RenderedImages(rdd.map { case (k,t) => (k, paintTile(k,t)) })
    }
  }
}
