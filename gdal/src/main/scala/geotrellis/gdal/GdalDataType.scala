package geotrellis.gdal

import org.gdal.gdal.gdal

object GdalDataType {
  val types =
    List(TypeUnknown,ByteConstantNoDataCellType, TypeUInt16,IntConstantNoDataCellType16,TypeUInt32,IntConstantNoDataCellType32,
         FloatConstantNoDataCellType32,FloatConstantNoDataCellType64,TypeCInt16,TypeCInt32,TypeCFloat32,
         TypeCFloat64)

  implicit def intToGdalDataType(i: Int): GdalDataType =
    types.find(_.code == i) match {
      case Some(dt) => dt
      case None => sys.error(s"Invalid GDAL data type code: $i")
    }

  implicit def GdalDataTypeToInt(typ: GdalDataType): Int =
    typ.code
}

abstract sealed class GdalDataType(val code: Int) {
  override
  def toString: String = gdal.GetDataTypeName(code)
}

case object TypeUnknown extends GdalDataType(0)
case object ByteConstantNoDataCellType extends GdalDataType(1)
case object TypeUInt16 extends GdalDataType(2)
case object IntConstantNoDataCellType16 extends GdalDataType(3)
case object TypeUInt32 extends GdalDataType(4)
case object IntConstantNoDataCellType32 extends GdalDataType(5)
case object FloatConstantNoDataCellType32 extends GdalDataType(6)
case object FloatConstantNoDataCellType64 extends GdalDataType(7)
case object TypeCInt16 extends GdalDataType(8)
case object TypeCInt32 extends GdalDataType(9)
case object TypeCFloat32 extends GdalDataType(10)
case object TypeCFloat64 extends GdalDataType(11)
