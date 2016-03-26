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
     * Renders each tile as a PNG. Assumes tiles are already colors of RGBA values.
     */
    def renderPng(): RDD[(SpatialKey, Png)] =
      rdd.mapValues(_.renderPng())

    /**
     * Renders each tile as a PNG.
     *
     * @param colorMap    ColorMap to use when rendering tile values to color.
     */
    def renderPng(colorMap: ColorMap): RDD[(SpatialKey, Png)] =
      rdd.mapValues(_.renderPng(colorMap))

    /**
     * Renders each tile as a JPG. Assumes tiles are already colors of RGBA values.
     */
    def renderJpg(): RDD[(SpatialKey, Jpg)] =
      rdd.mapValues(_.renderJpg())

    /**
     * Renders each tile as a JPG.
     *
     * @param colorMap    ColorMap to use when rendering tile values to color.
     */
    def renderJpg(colorMap: ColorMap): RDD[(SpatialKey, Jpg)] =
      rdd.mapValues(_.renderJpg(colorMap))
  }

  implicit class SpatialTileLayerRDDRenderMethods[M: GetComponent[?, CRS]: GetComponent[?, LayoutDefinition]](val rdd: RDD[(SpatialKey, Tile)] with Metadata[M]) {
    /**
     * Renders each tile as a GeoTiff.
     */
    def renderGeoTiff(): RDD[(SpatialKey, Array[Byte])] =
      rdd.mapPartitions({ partition =>
        val transform = rdd.metadata.getComponent[LayoutDefinition].mapTransform
        val crs = rdd.metadata.getComponent[CRS]
        partition.map { case (key, tile) =>
          (key, GeoTiff(tile, transform(key), crs).toByteArray)
        }
      }, preservesPartitioning = true)
  }
}
