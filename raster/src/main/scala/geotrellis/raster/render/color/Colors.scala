package geotrellis.raster.render

sealed trait Color {
  def get: Int
  def red: Int
  def green: Int
  def blue: Int
  def alpha: Int
  def isOpaque: Boolean
  def isTransparent: Boolean
  def isGrey = (red == green) && (green == blue)
}

case class RGB(x: Int) extends Color {
  def get = x
  def red = (x >> 24) & 0xff
  def green = (x >> 16) & 0xff
  def blue = (x >> 8) & 0xff
  def alpha = 0xff
  def isOpaque = true
  def isTransparent = false
  def isGrey = (red == green) && (green == blue)
  def unzip = (red, green, blue, alpha)
  def toRGBA = new RGBA(x << 8) + 0xff
}


case class RGBA(x: Int) extends Color {
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
}

object RGB {
  def apply(i: Int): RGB = new RGB(i)

  def apply(r: Int, g: Int, b: Int): RGB = new RGB((r << 24) + (g << 16) + (b << 8))
}

object RGBA {
  def apply(i: Int) = new RGBA(i)

  def apply(r: Int, g: Int, b: Int, a: Int): RGBA = new RGBA((r << 24) + (g << 16) + (b << 8) + a)

  def apply(r: Int, g: Int, b: Int, a: Float): RGBA = {
    assert(0 <= a && a <= 100)
    rgba(r, g, b, (a * 2.55).toInt)
  }
}
