package geotrellis

package object raster {
  // Keep constant values in sync with macro functions
  @inline final val byteNODATA = Byte.MinValue 
  @inline final val shortNODATA = Short.MinValue
  @inline final val NODATA = Int.MinValue

  import geotrellis.Macros._
  def isNoData(b:Byte):Boolean = macro isNoDataByte_impl
  def isNoData(s:Short):Boolean = macro isNoDataShort_impl
  def isNoData(i:Int):Boolean = macro isNoDataInt_impl
  def isNoData(f:Float):Boolean = macro isNoDataFloat_impl
  def isNoData(d:Double):Boolean = macro isNoDataDouble_impl

  def isData(b:Byte):Boolean = macro isDataByte_impl
  def isData(s:Short):Boolean = macro isDataShort_impl
  def isData(i:Int):Boolean = macro isDataInt_impl
  def isData(f:Float):Boolean = macro isDataFloat_impl
  def isData(d:Double):Boolean = macro isDataDouble_impl

  @inline final def b2i(n:Byte):Int = if (isNoData(n)) NODATA else n.toInt
  @inline final def i2b(n:Int):Byte = if (isNoData(n)) byteNODATA else n.toByte

  @inline final def s2i(n:Short):Int = if (isNoData(n)) NODATA else n.toInt
  @inline final def i2s(n:Int):Short = if (isNoData(n)) shortNODATA else n.toShort

  @inline final def i2f(n:Int):Float = if (isNoData(n)) Float.NaN else n.toFloat
  @inline final def f2i(n:Float):Int = if (isNoData(n)) NODATA else n.toInt

  @inline final def i2d(n:Int):Double = if (isNoData(n)) Double.NaN else n.toDouble
  @inline final def d2i(n:Double):Int = if (isNoData(n)) NODATA else n.toInt

  // Use this implicit class to fill arrays ... much faster than Array.fill[Int](dim)(val), etc.
  implicit class ByteArrayFiller(val arr:Array[Byte]) extends AnyVal {
    def fill(v:Byte) = { java.util.Arrays.fill(arr,v) ; arr }
  }
  implicit class ShortArrayFiller(val arr:Array[Short]) extends AnyVal {
    def fill(v:Short) = { java.util.Arrays.fill(arr,v) ; arr }
  }
  implicit class IntArrayFiller(val arr:Array[Int]) extends AnyVal {
    def fill(v:Int) = { java.util.Arrays.fill(arr,v) ; arr }
  }
  implicit class FloatArrayFiller(val arr:Array[Float]) extends AnyVal {
    def fill(v:Float) = { java.util.Arrays.fill(arr,v) ; arr }
  }
  implicit class DoubleArrayFiller(val arr:Array[Double]) extends AnyVal {
    def fill(v:Double) = { java.util.Arrays.fill(arr,v) ; arr }
  }

  type Png = Array[Byte]

  implicit class TraversableRasterExtentsion(rs: Traversable[Tile]) {
    def assertEqualDimensions(): Unit =
      if(Set(rs.map(_.dimensions)).size != 1) {
        val dimensions = rs.map(_.dimensions).toSeq
        throw new GeoAttrsError("Cannot combine tiles with different dimensions." +
          s"$dimensions are not all equal")
      }
  }
}
