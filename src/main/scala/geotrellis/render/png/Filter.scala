package geotrellis.render.png

sealed abstract class Filter(val n:Byte)
case object NoFilter extends Filter(0)
case object SubFilter extends Filter(1)
case object UpFilter extends Filter(2)
case object AvgFilter extends Filter(3)
case object PaethFilter extends Filter(4)
