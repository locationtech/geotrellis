package geotrellis.raster.render

class RGBA(val int: Int) extends AnyVal {
  def red = (int >> 24) & 0xff
  def green = (int >> 16) & 0xff
  def blue = (int >> 8) & 0xff
  def alpha = int & 0xff
  def isOpaque = (alpha == 255)
  def isTransparent = (alpha == 0)
  def isGrey = (red == green) && (green == blue)
  def unzip = (red, green, blue, alpha)
  def toRGB = new RGBA(int >> 8)
  def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
  def unzipRGB: (Int, Int, Int) = (red, green, blue)
}

object RGB {
  def apply(i: Int) = RGBA((i << 8) + 0xff)
}

object RGBA {
  def apply(i: Int) = new RGBA(i)

  def apply(r: Int, g: Int, b: Int, a: Int): RGBA = new RGBA((r << 24) + (g << 16) + (b << 8) + a)

  def apply(r: Int, g: Int, b: Int, alphaPct: Double): RGBA = {
    assert(0 <= alphaPct && alphaPct <= 100)
    RGBA(r, g, b, (alphaPct * 2.55).toInt)
  }
}
