package geotrellis.raster

import geotrellis.NODATA
import geotrellis._

/**
 * These can now be imported through the package object geotrellis.raster, e.g.
 * 
 * import geotrellis.raster._
 * 
 * The RasterUtil object contains a bunch of final values and methods used for
 * no data checks and conversions. It's important to avoid using toInt and
 * toDouble when converting raster values, since these methods don't have
 * NODATA/Double.NaN conversion correctly.
 * 
 */
object RasterUtil {
  final val byteNodata = Byte.MinValue
  final val shortNodata = Short.MinValue

  @inline final def isNodata(n:Int) = n == NODATA
  @inline final def isNodata(n:Double) = java.lang.Double.isNaN(n)

  @inline final def isData(n:Int) = n != NODATA
  @inline final def isData(n:Double) = !java.lang.Double.isNaN(n)

  @inline final def b2i(n:Byte):Int = if (isNoData(n)) NODATA else n.toInt
  @inline final def i2b(n:Int):Byte = if (isNoData(n)) byteNodata else n.toByte

  @inline final def s2i(n:Short):Int = if (n == shortNodata) NODATA else n.toInt
  @inline final def i2s(n:Int):Short = if (isNoData(n)) shortNodata else n.toShort

  @inline final def i2f(n:Int):Float = if (isNoData(n)) Float.NaN else n.toFloat
  @inline final def f2i(n:Float):Int = if (java.lang.Float.isNaN(n)) NODATA else n.toInt

  @inline final def i2d(n:Int):Double = if (n == NODATA) Double.NaN else n.toDouble
  @inline final def d2i(n:Double):Int = if (java.lang.Double.isNaN(n)) NODATA else n.toInt
}
