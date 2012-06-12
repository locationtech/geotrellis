package geotrellis

sealed abstract class RasterType(val precedence:Int, val float:Boolean, val name:String) {
  def bits = if (float) precedence / 10 else precedence
  def union(rhs:RasterType) = if (precedence < rhs.precedence) rhs else this
  def intersect(rhs:RasterType) = if (precedence < rhs.precedence) this else rhs

  def contains(rhs:RasterType) = precedence >= rhs.precedence
}

case object TypeBit extends RasterType(1, false, "bool")
case object TypeByte extends RasterType(8, false, "int8")
case object TypeShort extends RasterType(16, false, "int16")
case object TypeInt extends RasterType(32, false, "int32")
case object TypeFloat extends RasterType(320, true, "float32")
case object TypeDouble extends RasterType(640, true, "float64")
