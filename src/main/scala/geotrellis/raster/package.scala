package geotrellis

import geotrellis.source.RasterDataSource
package object raster {

  final val byteNodata = Byte.MinValue
  final val shortNodata = Short.MinValue

  @inline final def isNodata(n:Int) = n == NODATA
  @inline final def isNodata(n:Double) = java.lang.Double.isNaN(n)

  @inline final def isData(n:Int) = n != NODATA
  @inline final def isData(n:Double) = !java.lang.Double.isNaN(n)

  @inline final def b2i(n:Byte):Int = if (n == byteNodata) NODATA else n.toInt
  @inline final def i2b(n:Int):Byte = if (n == NODATA) byteNodata else n.toByte

  @inline final def b2d(n:Byte):Double = if (n == byteNodata) Double.NaN else n.toDouble
  @inline final def d2b(n:Double):Byte = if (java.lang.Double.isNaN(n)) byteNodata else n.toByte

  @inline final def s2i(n:Short):Int = if (n == shortNodata) NODATA else n.toInt
  @inline final def i2s(n:Int):Short = if (n == NODATA) shortNodata else n.toShort

  @inline final def s2d(n:Short):Double = if (n == shortNodata) Double.NaN else n.toDouble
  @inline final def d2s(n:Double):Short = if (java.lang.Double.isNaN(n)) shortNodata else n.toShort

  @inline final def i2f(n:Int):Float = if (n == NODATA) Float.NaN else n.toFloat
  @inline final def f2i(n:Float):Int = if (java.lang.Float.isNaN(n)) NODATA else n.toInt

  @inline final def i2d(n:Int):Double = if (n == NODATA) Double.NaN else n.toDouble
  @inline final def d2i(n:Double):Int = if (java.lang.Double.isNaN(n)) NODATA else n.toInt  
}
