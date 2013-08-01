package geotrellis.raster

import geotrellis._

/**
 * LazyConvert represents a lazily-applied conversion to any type.
 *
 * @note     If you care converting to a RasterType with less bits
 *           than the type of the underlying data, you are responsible
 *           for managing overflow. This convert does not do any casting;
 *           therefore converting from a TypeInt to TypeByte could still
 *           return values greater than 127 from apply().
 */
final case class LazyConvert(data: ArrayRasterData, typ: RasterType)
  extends LazyRasterData {

  def cols = data.cols
  def rows = data.rows

  def getType = typ
  def alloc(cols: Int, rows: Int) = RasterData.allocByType(typ, cols, rows)
  def length = data.length
  def apply(i: Int) = data.apply(i)
  def applyDouble(i: Int) = data.applyDouble(i)
  def copy = this
  override def toArray = data.toArray
  override def toArrayDouble = data.toArrayDouble

  def foreach(f: Int => Unit) = data.foreach(f)
  def map(f: Int => Int) = LazyMap(this, f)
  def mapIfSet(f: Int => Int) = LazyMapIfSet(this, f)
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(this, a, f)
    case o                  => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreachDouble(f)
  def mapDouble(f: Double => Double) = LazyMapDouble(this, f)
  def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(this, a, f)
    case o                  => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
