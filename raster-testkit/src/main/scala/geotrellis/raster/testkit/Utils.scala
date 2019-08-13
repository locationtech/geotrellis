package geotrellis.raster.testkit

import geotrellis.raster.{CellGrid, Raster}
import geotrellis.vector.Extent

import scala.util.Try

object Utils {
  def roundRaster[T <: CellGrid[Int]](raster: Raster[T], scale: Int = 11): Raster[T] =
    raster.copy(extent = roundExtent(raster.extent, scale))

  def roundExtent(extent: Extent, scale: Int = 11): Extent = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    Extent(
      BigDecimal(xmin).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(ymin).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(xmax).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble,
      BigDecimal(ymax).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble
    )
  }

  /** A dirty reflection function to modify object vals */
  def modifyField(obj: AnyRef, name: String, value: Any) {
    def impl(clazz: Class[_]) {
      Try(clazz.getDeclaredField(name)).toOption match {
        case Some(field) =>
          field.setAccessible(true)
          clazz.getMethod(name).invoke(obj) // force init in case it's a lazy val
          field.set(obj, value) // overwrite value
        case None =>
          if (clazz.getSuperclass != null) {
            impl(clazz.getSuperclass)
          }
      }
    }

    impl(obj.getClass)
  }
}
