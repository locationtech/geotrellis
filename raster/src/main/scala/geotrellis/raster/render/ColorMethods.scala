package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.op.stats._

import java.awt.image.BufferedImage

import spire.syntax.cfor._
trait ColorMethods extends TileMethods {
  def color(colorClassifier: ColorClassifier[_]): Tile =
    colorClassifier.toColorMap().render(tile)

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
