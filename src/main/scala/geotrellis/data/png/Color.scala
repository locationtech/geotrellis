package geotrellis.data.png

sealed abstract class Color(val n:Byte, val depth:Int)

// greyscale and color opaque rasters
case class Grey(transparent:Int) extends Color(0, 1)
case class Rgb(transparent:Int) extends Color(2, 3)

// indexed color, using separate rgb and alpha channels
case class Indexed(rgbs:Array[Int], as:Array[Int]) extends Color(3, 1)

// greyscale and color rasters with an alpha byte
case object Greya extends Color(4, 4)
case object Rgba extends Color(6, 4)
