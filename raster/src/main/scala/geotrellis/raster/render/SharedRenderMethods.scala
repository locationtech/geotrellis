package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.op.stats._

import java.awt.image.BufferedImage

import spire.syntax.cfor._

trait SharedRenderMethods extends TileMethods {
  def color(breaksToColors: Map[Int, Int]): Tile =
    IntColorMap(breaksToColors).render(tile)

  def color(breaksToColors: Map[Int, Int], options: ColorMapOptions): Tile =
    IntColorMap(breaksToColors, options).render(tile)

  def color(breaksToColors: Map[Double, Int])(implicit d: DI): Tile =
    DoubleColorMap(breaksToColors).render(tile)

  def color(breaksToColors: Map[Double, Int], options: ColorMapOptions)(implicit d: DI): Tile =
    DoubleColorMap(breaksToColors, options).render(tile)

  def color(colorBreaks: ColorBreaks): Tile =
    colorBreaks.toColorMap.render(tile)

  def color(colorBreaks: ColorBreaks, options: ColorMapOptions): Tile =
    colorBreaks.toColorMap(options).render(tile)

  def toBufferedImage: BufferedImage = {
    val bi = new BufferedImage(tile.cols, tile.rows, BufferedImage.TYPE_INT_RGB)
    cfor(0)(_ < tile.cols, _ + 1) { x =>
      cfor(0)(_ < tile.rows, _ + 1) { y =>
        bi.setRGB(x, y, tile.get(x, y))
      }
    }
    bi
  }
}
