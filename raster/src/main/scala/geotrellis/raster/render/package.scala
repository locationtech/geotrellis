package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  // RGB and RGBA colors

  implicit class RGBA(val int: Int) extends AnyVal {
    def red = (int >> 24) & 0xff
    def green = (int >> 16) & 0xff
    def blue = (int >> 8) & 0xff
    def alpha = int & 0xff
    def isOpaque = (alpha == 255)
    def isTransparent = (alpha == 0)
    def isGrey = (red == green) && (green == blue)
    def unzip = (red, green, blue, alpha)
    def toRGB = int >> 8
    def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
    def unzipRGB: (Int, Int, Int) = (red, green, blue)
  }

  object RGB {
    def apply(i: Int): Int = (i << 8) + 0xff

    def apply(r: Int, g: Int, b: Int, a: Int): Int = apply((r << 24) + (g << 16) + (b << 8))
  }

  object RGBA {
    def apply(r: Int, g: Int, b: Int, a: Int): Int = new RGBA((r << 24) + (g << 16) + (b << 8) + a).int

    def apply(r: Int, g: Int, b: Int, alphaPct: Double): Int = {
      assert(0 <= alphaPct && alphaPct <= 100)
      RGBA(r, g, b, (alphaPct * 2.55).toInt).int
    }
  }

  // implicit class IntColorClassifierToColorRampCachingMethods(val self: ColorClassifier[Int]) {
  //   /** Converts this color classifier based on a histogram. This is an optimization */
  //   def toColorMap(histogram: Histogram[Int]): ColorMap = {
  //     val colorMap: IntColorMap = ColorMap(self.getBreaks, self.getColors.map(_.int), self.cmapOptions)
  //     colorMap.cache(histogram)
  //   }
  // }
}
