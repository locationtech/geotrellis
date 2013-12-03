 package geotrellis.render.png

import geotrellis._
import geotrellis.render._

import scala.collection.mutable

case class Renderer(colorMap:IntColorMap, rasterType:RasterType, colorType:ColorType) {
  def render(r:Raster) = 
      r.convert(rasterType).map(colorMap)
  def settings = Settings(colorType, PaethFilter)
}

object Renderer {
  def apply(breaks:ColorBreaks, nodata:Int):Renderer = {
    apply(breaks.limits, breaks.colors, nodata)
  }

  def apply(limits:Array[Int], colors:Array[Int], nodata:Int):Renderer = {
    val n = limits.length
    if(colors.length < 255) {
      val indices = (0 until colors.length).toArray
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
      val colorType = Indexed(rgbs, as)
      val colorMap = ColorMap(limits,indices,ColorMapOptions(LessThan,255))
      Renderer(colorMap, TypeByte, colorType)
    } else {

      var opaque = true
      var grey = true
      var i = 0
      while (i < colors.length) {
        val c = colors(i)
        opaque &&= Color.isOpaque(c)
        grey &&= Color.isGrey(c)
        i += 1
      }

      if (grey && opaque) {
        val colorMap = ColorMap(limits,colors.map(z => (z >> 8) & 0xff),ColorMapOptions(LessThan,nodata))
        Renderer(colorMap, TypeByte, Grey(nodata))
      } else if (opaque) {
        val colorMap = ColorMap(limits,colors.map(z => z >> 8),ColorMapOptions(LessThan,nodata))
        Renderer(colorMap, TypeInt, Rgb(nodata))
      } else if (grey) {
        val colorMap = ColorMap(limits,colors.map(z => z & 0xffff),ColorMapOptions(LessThan,nodata))
        Renderer(colorMap, TypeShort, Greya)
      } else {
        val colorMap = ColorMap(limits,colors,ColorMapOptions(LessThan,nodata))
        Renderer(colorMap, TypeInt, Rgba)
      }
    }
  }
}
