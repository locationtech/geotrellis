package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.summary._
import geotrellis.util.MethodExtensions

import java.awt.image.BufferedImage

import spire.syntax.cfor._

trait ColorMethods extends MethodExtensions[Tile] {
  def color(colorMap: ColorMap): Tile =
    colorMap.render(self)

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

trait MultibandColorMethods extends MethodExtensions[MultibandTile] {
  /** Turns an RGB or an RGBA multiband tile into a integer packed RGBA single band tile */
  def color(): Tile = {
    assert(self.bandCount == 3 || self.bandCount == 4)

    if(self.bandCount == 3) {
      self.convert(IntConstantNoDataCellType).combine(0, 1, 2) { (rBand, gBand, bBand) =>
        val r = if (isData(rBand)) { rBand } else 0
        val g = if (isData(gBand)) { gBand } else 0
        val b = if (isData(bBand)) { bBand } else 0

        if(r + g + b == 0) 0
        else {
          ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | 0xFF
        }
      }
    } else {
      self.convert(IntConstantNoDataCellType).combine(0, 1, 2, 3) { (rBand, gBand, bBand, aBand) =>
        val r = if (isData(rBand)) { rBand } else 0
        val g = if (isData(gBand)) { gBand } else 0
        val b = if (isData(bBand)) { bBand } else 0
        val a = if (isData(aBand)) { aBand } else 0

        if(r + g + b == 0) 0
        else {
          ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF)
        }
      }
    }
  }
}
