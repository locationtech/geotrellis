package geotrellis

sealed abstract class RasterType(val precedence:Int, val float:Boolean, val name:String) extends Serializable {
  def bits = if (float) precedence / 10 else precedence
  def bytes = bits / 8
  def union(rhs:RasterType) = if (precedence < rhs.precedence) rhs else this
  def intersect(rhs:RasterType) = if (precedence < rhs.precedence) this else rhs

  def contains(rhs:RasterType) = precedence >= rhs.precedence

  def isDouble = precedence > 32

  def numBytes(size:Int) = bytes * size
}

case object TypeBit extends RasterType(1, false, "bool") { 
  override final def numBytes(size:Int) = (size + 7) / 8
}

case object TypeByte extends RasterType(8, false, "int8")
case object TypeShort extends RasterType(16, false, "int16")
case object TypeInt extends RasterType(32, false, "int32")
case object TypeFloat extends RasterType(320, true, "float32")
case object TypeDouble extends RasterType(640, true, "float64")
