package geotrellis.render.png

sealed abstract class ColorType(val n:Byte, val depth:Int)

// greyscale and color opaque rasters
case class Grey(transparent:Int) extends ColorType(0, 1)
case class Rgb(transparent:Int) extends ColorType(2, 3)

// indexed color, using separate rgb and alpha channels
case class Indexed(rgbs:Array[Int], as:Array[Int]) extends ColorType(3, 1)

// greyscale and color rasters with an alpha byte
case object Greya extends ColorType(4, 4)
case object Rgba extends ColorType(6, 4)
