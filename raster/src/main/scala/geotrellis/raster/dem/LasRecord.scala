package geotrellis.raster.dem

/*
 * These types are taken from Table 12 of [1].
 *
 * 1. http://www.asprs.org/wp-content/uploads/2010/12/LAS_1_4_r13.pdf
 */

sealed abstract class LasRecord
sealed abstract class NumericalLasRecord extends LasRecord
sealed case class CategoricalLasRecord(data: Array[String]) extends LasRecord
sealed case class IntegralLasRecord(data: Array[Int]) extends NumericalLasRecord
sealed case class FloatingLasRecord(data: Array[Double]) extends NumericalLasRecord
