package geotrellis.raster.dem

/*
 * These types are taken from Table 12 of [1].
 *
 * 1. http://www.asprs.org/wp-content/uploads/2010/12/LAS_1_4_r13.pdf
 */

sealed abstract class LasRecordType

sealed abstract class NumericalLasRecordType extends LasRecordType
sealed abstract class CategoricalLasRecordType extends LasRecordType
sealed abstract class IntegralLasRecordType extends NumericalLasRecordType
sealed abstract class FloatingLasRecordType extends NumericalLasRecordType

object Z extends IntegralLasRecordType

object Red extends IntegralLasRecordType
object Green extends IntegralLasRecordType
object Blue extends IntegralLasRecordType

object Classification extends IntegralLasRecordType
object EdgeOfFlightLine extends IntegralLasRecordType
object Intensity extends IntegralLasRecordType
object NumberOfReturns extends IntegralLasRecordType
object ReturnNumber extends IntegralLasRecordType
object ScanAngle extends IntegralLasRecordType
object ScanDirection extends IntegralLasRecordType

object GpsTime extends FloatingLasRecordType

object UserData extends CategoricalLasRecordType
object PointSourceId extends CategoricalLasRecordType
