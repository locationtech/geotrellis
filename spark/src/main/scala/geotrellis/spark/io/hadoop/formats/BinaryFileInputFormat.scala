package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import org.apache.hadoop.fs._

import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

class BinaryFileRecordReader[K, V](read: Array[Byte] => (K, V)) extends RecordReader[K, V] {
  private var tup: (K, V) = null
  private var hasNext: Boolean = true

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val path = split.asInstanceOf[FileSplit].getPath()
    val conf = context.getConfiguration()
    val bytes = HdfsUtils.readBytes(path, conf)

    tup = read(bytes)
  }

  def close = {}
  def getCurrentKey = tup._1
  def getCurrentValue = { hasNext = false ; tup._2 }
  def getProgress = 1
  def nextKeyValue = hasNext
}

trait BinaryFileInputFormat[K, V] extends FileInputFormat[K, V] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (K, V)

  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[K, V] = 
    new BinaryFileRecordReader({ bytes => read(bytes, context) })
}
