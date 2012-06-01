package geotrellis

sealed trait RasterType
case object TypeBit extends RasterType
case object TypeByte extends RasterType
case object TypeShort extends RasterType
case object TypeInt extends RasterType
case object TypeFloat extends RasterType
case object TypeDouble extends RasterType
