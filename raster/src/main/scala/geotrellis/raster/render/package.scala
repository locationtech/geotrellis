package geotrellis.raster

import geotrellis.raster.histogram._

package object render {
  // RGB and RGBA
  // Note: GeoTrellis by default expects colors to be in RGBA format.

  implicit class RGBA(val int: Int) extends AnyVal {
    def red = (int >> 24) & 0xff
    def green = (int >> 16) & 0xff
    def blue = (int >> 8) & 0xff
    def alpha = int & 0xff
    def isOpaque = (alpha == 255)
    def isTransparent = (alpha == 0)
    def isGrey = (red == green) && (green == blue)
    def unzip = (red, green, blue, alpha)
    def toARGB = (int >> 8) | (alpha << 24)
    def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
    def unzipRGB: (Int, Int, Int) = (red, green, blue)
  }

  object RGB {
    def apply(r: Int, g: Int, b: Int): Int = ((r << 24) + (g << 16) + (b << 8)) | 0xFF
  }

  object RGBA {
    def apply(r: Int, g: Int, b: Int, a: Int): Int =
      new RGBA((r << 24) + (g << 16) + (b << 8) + a).int

    def apply(r: Int, g: Int, b: Int, alphaPct: Double): Int = {
      assert(0 <= alphaPct && alphaPct <= 100)
      RGBA(r, g, b, (alphaPct * 2.55).toInt)
    }
  }
}
