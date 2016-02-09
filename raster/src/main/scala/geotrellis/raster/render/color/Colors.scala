package geotrellis.raster.render

class RGBA(val x: Int) extends AnyVal {
  def get = x
  def red = (x >> 24) & 0xff
  def green = (x >> 16) & 0xff
  def blue = (x >> 8) & 0xff
  def alpha = x & 0xff
  def isOpaque = (alpha == 255)
  def isTransparent = (alpha == 0)
  def isGrey = (red == green) && (green == blue)
  def unzip = (red, green, blue, alpha)
  def toRGB = new RGBA(x >> 8)
  def unzipRGBA = (red, green, blue, alpha)
  def unzipRGB = (red, green, blue)
}

object RGB {
  def apply(i: Int) = RGBA((i << 8) + 0xff)
}

object RGBA {
  def apply(i: Int) = new RGBA(i)

  def apply(r: Int, g: Int, b: Int, a: Int): RGBA = new RGBA((r << 24) + (g << 16) + (b << 8) + a)

  def apply(r: Int, g: Int, b: Int, a: Float): RGBA = {
    assert(0 <= a && a <= 100)
    RGBA(r, g, b, (a * 2.55).toInt)
  }
}
