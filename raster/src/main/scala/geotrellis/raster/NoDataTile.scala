package geotrellis.raster

object NoDataTile {
  def apply(cellType: CellType, cols: Int, rows: Int): ConstantTile =
    cellType match {
      case TypeBit => throw new IllegalArgumentException("TypeBit tiles can not express NODATA")
      case TypeByte => ByteConstantTile(byteNODATA, cols, rows)
      case TypeShort => ShortConstantTile(shortNODATA, cols, rows)
      case TypeInt => IntConstantTile(NODATA, cols, rows)
      case TypeFloat => FloatConstantTile(Float.NaN, cols, rows)
      case TypeDouble => DoubleConstantTile(Double.NaN, cols, rows)
    }
}


