package geotrellis.raster.io.geotiff

import geotrellis.raster.CellType

trait GeoTiffSegment {
  def size: Int
  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  def bytes: Array[Byte]

  def convert(cellType: CellType): Array[Byte]

  def map(f: Int => Int): Array[Byte]
  def mapDouble(f: Double => Double): Array[Byte]
  def mapWithIndex(f: (Int, Int) => Int): Array[Byte]
  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte]
}
