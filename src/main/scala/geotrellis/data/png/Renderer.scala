package geotrellis.data.png

import geotrellis._
import geotrellis.data._
import geotrellis.statistics.Histogram

case class Renderer(limits:Array[Int], colors:Array[Int], histogram:Histogram,
                    nodata:Int, rasterType:RasterType, color:Color)
extends Function1[Int, Int] {
  def makeColorMap() = {
    val ch = histogram.copy
    val len = limits.length - 1
    def findColor(z:Int):Int = {
      var i = 0
      while (i < len) {
        if (z <= limits(i)) return colors(i)
        i += 1
      }
      colors(len)
    }
    histogram.foreachValue(z => ch.setItem(z, findColor(z)))
    ch
  }

  private val colorMap = makeColorMap()

  def settings = Settings(color, PaethFilter)
  def render(r:Raster) = r.convert(rasterType).map(this)
  def apply(z:Int):Int = { if(z == NODATA) nodata else colorMap.getItemCount(z) }
}

object Renderer {
  def apply(breaks:ColorBreaks, h:Histogram, nodata:Int):Renderer = {
    apply(breaks.limits, breaks.colors, h, nodata)
  }

  def apply(limits:Array[Int], colors:Array[Int], h:Histogram, nodata:Int):Renderer = {
    val n = limits.length
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
      return Renderer(limits, indices, h, 255, TypeByte, Indexed(rgbs, as))
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

    if (grey && opaque) {
      Renderer(limits, colors.map(z => (z >> 8) & 0xff), h, nodata, TypeByte, Grey(nodata))
    } else if (opaque) {
      Renderer(limits, colors.map(z => z >> 8), h, nodata, TypeInt, Rgb(nodata))
    } else if (grey) {
      Renderer(limits, colors.map(z => z & 0xffff), h, nodata, TypeShort, Greya)
    } else {
      Renderer(limits, colors, h, nodata, TypeInt, Rgba)
    }
  }
}
