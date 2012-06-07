package geotrellis

sealed abstract class RasterType(val precedence:Int, val float:Boolean) {
  def bits = if (float) precedence / 10 else precedence
  def union(rhs:RasterType) = if (precedence < rhs.precedence) rhs else this
  def intersect(rhs:RasterType) = if (precedence < rhs.precedence) this else rhs

  def contains(rhs:RasterType) = precedence >= rhs.precedence
}

case object TypeBit extends RasterType(1, false)
case object TypeByte extends RasterType(8, false)
case object TypeShort extends RasterType(16, false)
case object TypeInt extends RasterType(32, false)
case object TypeFloat extends RasterType(320, true)
case object TypeDouble extends RasterType(640, true)
