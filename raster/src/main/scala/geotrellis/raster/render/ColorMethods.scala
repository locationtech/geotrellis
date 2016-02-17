package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.summary._

import java.awt.image.BufferedImage

import spire.syntax.cfor._

trait ColorMethods extends MethodExtensions[Tile] {
  def color(breaksToColors: Map[Int, Int]): Tile =
    IntColorMap(breaksToColors).render(self)

  def color(breaksToColors: Map[Int, Int], options: ColorMapOptions): Tile =
    IntColorMap(breaksToColors, options).render(self)

  def color(breaksToColors: Map[Double, Int])(implicit d: DI): Tile =
    DoubleColorMap(breaksToColors).render(self)

  def color(breaksToColors: Map[Double, Int], options: ColorMapOptions)(implicit d: DI): Tile =
    DoubleColorMap(breaksToColors, options).render(self)

  def color(colorBreaks: ColorBreaks): Tile =
    colorBreaks.toColorMap.render(self)

  def color(colorBreaks: ColorBreaks, options: ColorMapOptions): Tile =
    colorBreaks.toColorMap(options).render(self)

  def toBufferedImage: BufferedImage = {
    val bi = new BufferedImage(self.cols, self.rows, BufferedImage.TYPE_INT_RGB)
    cfor(0)(_ < self.cols, _ + 1) { x =>
      cfor(0)(_ < self.rows, _ + 1) { y =>
        bi.setRGB(x, y, self.get(x, y))
      }
    }
    bi
  }
}
