package geotrellis.raster.render

class RGBA(val x: Int) extends AnyVal {
  def red = (x >> 24) & 0xff
  def green = (x >> 16) & 0xff
  def blue = (x >> 8) & 0xff
  def alpha = x & 0xff
  def isOpaque = (alpha == 255)
  def isTransparent = (alpha == 0)
  def isGrey = (red == green) && (green == blue)
  def unzip = (red, green, blue, alpha)
  def toRGB = new RGB(x >> 8)
}

object RGBA {
  def apply(r: Int, g: Int, b: Int, a: Int): RGBA = new RGBA((r << 24) + (g << 16) + (b << 8) + a)

  def apply(r: Int, g: Int, b: Int, a: Float): RGBA = {
    assert(0 <= a && a <= 100)
    rgba(r, g, b, (a * 2.55).toInt)
  }
}
