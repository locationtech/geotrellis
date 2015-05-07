package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._

import org.joda.time.{DateTimeZone, DateTime}

class SpaceTimeFilterMapFileInputFormat extends FilterMapFileInputFormat[SpaceTimeKey, SpaceTimeKeyWritable, TileWritable] {
  def createKey() = new SpaceTimeKeyWritable
  def createKey(index: Long) = SpaceTimeKeyWritable(index, SpaceTimeKey(0, 0, new DateTime(0,0,0)))
  def createValue() = new TileWritable
}
