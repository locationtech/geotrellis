package geotrellis.data.png

sealed abstract class Color(val n:Byte, val depth:Int)
case object Rgba extends Color(Const.rgba, 4)
case class Rgb(background:Int, transparent:Int) extends Color(Const.rgb, 3)
