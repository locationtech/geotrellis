package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

import scalaxy.loops._

import scala.collection.mutable

trait ZonalOpMethods[+Repr <: RasterSource] { self:Repr =>
  /**
   * Given a raster, return a histogram summary of the cells within each zone.
   *
   * @note    zonalHistorgram does not currently support Double raster data.
   *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
   *          the data values will be rounded to integers.
   */
  def zonalHistogram(zonesSource:RasterSource) =
    self.converge.mapOp(ZonalHistogram(_,zonesSource.get))

  /**
   * Given a raster and a raster representing it's zones, sets all pixels
   * within each zone to the percentage of those pixels having values equal 
   * to that of the given pixel.
   *
   * Percentages are integer values from 0 - 100.
   * 
   * @note    ZonalPercentage does not currently support Double raster data.
   *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
   *          the data values will be rounded to integers.
   */
  def zonalPercentage(zonesSource:RasterSource) =
    self.converge.mapOp(ZonalPercentage(_,zonesSource.get))
}
