package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex

import com.github.nscala_time.time.Imports._

import scala.collection._
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap

object ZSpaceTimeKeyIndex {
  private val functions: concurrent.Map[String, (String, DateTime) => Int] = new ConcurrentHashMap[String, (String, DateTime) => Int]

  List[(String, (String, DateTime) => Int)](
    "pattern" -> { (p, dt) => DateTimeFormat.forPattern(p).print(dt).toInt },
    "year"    -> { (_, dt) => dt.getYear },
    "month"   -> { (_, dt) => f"${dt.getYear}${dt.getMonthOfYear}%02d".toInt },
    "day"     -> { (_, dt) => f"${dt.getYear}${dt.getDayOfYear}%03d".toInt },
    "hour"    -> { (_, dt) => f"${dt.getYear}${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d".toInt },
    "minute"  -> { (_, dt) => f"${dt.getYear}${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d${dt.getMinuteOfHour}%02d".toInt },
    "second"  -> { (_, dt) => f"${dt.getYear}${dt.getDayOfYear}%03d${dt.getHourOfDay}%02d${dt.getMinuteOfHour}%02d${dt.getSecondOfMinute}%02d".toInt },
    "millis"  -> { (_, dt) => dt.getMillis.toInt }
  ).map(functions += _)

  def addCustomFunction(fname: String, function: DateTime => Int) =
    functions.update(fname, Function.uncurried({ (str: String) => function(_) }))

  case class Options(fname: String, pattern: String) {
    def timeToGrid: DateTime => Int = functions(fname) curried pattern
  }

  object Options {
    val DEFAULT = Options("function")

    def apply(fname: String): Options = Options(fname, "")

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
