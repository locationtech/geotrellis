package geotrellis.spark.render

import geotrellis.raster.Tile
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

trait SpatialTileRDDRenderMethods extends MethodExtensions[RDD[(SpatialKey, Tile)]] {
  def color(colorMap: ColorMap): RDD[(SpatialKey, Tile)] =
    self.mapValues(_.color(colorMap))

  /**
    * Renders each tile as a PNG. Assumes tiles are already colors of RGBA values.
    */
  def renderPng(): RDD[(SpatialKey, Png)] =
    Render.renderPng(self)

  /**
    * Renders each tile as a PNG.
    *
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderPng(colorMap: ColorMap): RDD[(SpatialKey, Png)] =
    Render.renderPng(self, colorMap)

  /**
    * Renders each tile as a JPG. Assumes tiles are already colors of RGBA values.
    */
  def renderJpg(): RDD[(SpatialKey, Jpg)] =
    Render.renderJpg(self)

  /**
    * Renders each tile as a JPG.
    *
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderJpg(colorMap: ColorMap): RDD[(SpatialKey, Jpg)] =
    Render.renderJpg(self, colorMap)
}
