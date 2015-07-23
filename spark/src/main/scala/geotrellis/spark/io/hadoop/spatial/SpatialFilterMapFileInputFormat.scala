package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import scala.reflect.ClassTag

class SpatialFilterMapFileInputFormat extends FilterMapFileInputFormat[SpatialKey, SpatialKeyWritable, KryoWritable] {
  def createKey() = new SpatialKeyWritable
  def createKey(index: Long) = SpatialKeyWritable(index, SpatialKey(0, 0))
  def createValue() = new KryoWritable
}
