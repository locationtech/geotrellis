package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex

import com.github.nscala_time.time.Imports._
import scala.collection.immutable.Map

object ZSpaceTimeKeyIndex {
  val functions: Map[String, (String, DateTime) => Int] = Map(
    "pattern" -> { (p, dt) => DateTimeFormat.forPattern(p).print(dt).toInt },
    "year"    -> { (_, dt) => f"${dt.getYear}%04d".toInt },
    "month"   -> { (_, dt) => f"${dt.getYear}%04d${dt.getMonthOfYear}%02d".toInt },
    "day"     -> { (_, dt) => f"${dt.getYear}%04d${dt.getDayOfYear}%03d".toInt },
    "hour"    -> { (_, dt) => f"${dt.getYear}%04d${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d".toInt },
    "minute"  -> { (_, dt) => f"${dt.getYear}%04d${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d${dt.getMinuteOfHour}%02d".toInt },
    "second"  -> { (_, dt) => f"${dt.getYear}%04d${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d${dt.getMinuteOfHour}%02d${dt.getSecondOfMinute}%02d".toInt },
    "millis"  -> { (_, dt) => dt.getMillis.toInt }
  )

  case class Options(ftype: String, pattern: String) {
    def timeToGrid: DateTime => Int = functions(ftype) curried pattern
  }

  object Options {
    val DEFAULT = Options("function")

    def apply(ftype: String): Options = Options(ftype, "")

    def toIndex(opts: Options): ZSpaceTimeKeyIndex = new ZSpaceTimeKeyIndex(opts.timeToGrid, opts)
  }

  import Options._
  def apply(timeToGrid: DateTime => Int): KeyIndex[SpaceTimeKey] =
    new ZSpaceTimeKeyIndex(timeToGrid)

  def byYear(): ZSpaceTimeKeyIndex = toIndex(Options("year"))

  def byMonth(): ZSpaceTimeKeyIndex = toIndex(Options("month"))

  def byDay(): ZSpaceTimeKeyIndex = toIndex(Options("day"))

  def byPattern(pattern: String): ZSpaceTimeKeyIndex = toIndex(Options("pattern", pattern))
}

class ZSpaceTimeKeyIndex(timeToGrid: DateTime => Int, val persistentOptions: ZSpaceTimeKeyIndex.Options = ZSpaceTimeKeyIndex.Options.DEFAULT) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, timeToGrid(key.time))

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
