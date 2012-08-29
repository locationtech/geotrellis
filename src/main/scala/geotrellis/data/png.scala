package geotrellis.data.png

import geotrellis._

case class Renderer(breaks:Array[Int], colors:Array[Int], nodata:Int,
                    rasterType:RasterType, color:Color) extends Function1[Int, Int] {

  def settings = Settings(color, PaethFilter)

  def render(r:Raster) = r.convert(rasterType).map(this)

  def apply(z:Int):Int = {
    if (z == NODATA) return nodata
    val limit = breaks.length - 1
    var i = 0
    while (i < limit) {
      if (z <= breaks(i)) return colors(i)
      i += 1
    }
    colors(limit)
  }
}

object Renderer {
  def apply(breaks:Array[Int], colors:Array[Int], nodata:Int):Renderer = {
    val n = breaks.length
    if (n < 255) {
      val indices = (0 until n).toArray
      val rgbs = new Array[Int](256)
      val as = new Array[Int](256)

      var i = 0
      while (i < n) {
        val c = colors(i)
        rgbs(i) = c >> 8
        as(i) = c & 0xff
        i += 1
      }
      rgbs(255) = 0
      as(255) = 0

      return Renderer(breaks, indices, 255, TypeByte, Indexed(rgbs, as))
    }

    import geotrellis.data.Color._

    var opaque = true
    var grey = true
    var i = 0
    while (i < colors.length) {
      val c = colors(i)
      opaque &&= isOpaque(c)
      grey &&= isGrey(c)
      i += 1
    }

    if (grey && opaque) 
      Renderer(breaks, colors, nodata, TypeByte, Grey(nodata))
    else if (opaque)
      Renderer(breaks, colors, nodata, TypeInt, Rgb(nodata))
    else if (grey)
      Renderer(breaks, colors, nodata, TypeShort, Greya)
    else
      Renderer(breaks, colors, nodata, TypeInt, Rgba)
  }
}
