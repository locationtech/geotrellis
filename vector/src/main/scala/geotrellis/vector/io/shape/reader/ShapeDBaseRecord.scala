package geotrellis.vector.io.shape.reader

import java.util.Date

trait ShapeDBaseRecord

case class LogicalDBaseRecord(value: Boolean) extends ShapeDBaseRecord

case class StringDBaseRecord(value: String) extends ShapeDBaseRecord

case class DateDBaseRecord(value: Date) extends ShapeDBaseRecord

case class LongDBaseRecord(value: Long) extends ShapeDBaseRecord

case class DoubleDBaseRecord(value: Double) extends ShapeDBaseRecord
