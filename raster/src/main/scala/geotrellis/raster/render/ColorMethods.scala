package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.summary._
import geotrellis.util.MethodExtensions

import java.awt.image.BufferedImage

import spire.syntax.cfor._


trait ColorMethods extends MethodExtensions[Tile] {
  def color(colorClassifier: ColorClassifier[_]): Tile =
    colorClassifier.toColorMap().render(self)

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
