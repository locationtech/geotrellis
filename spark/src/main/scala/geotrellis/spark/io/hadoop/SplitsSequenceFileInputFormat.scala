package geotrellis.spark.io.hadoop

import org.apache.hadoop.io._
import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

class SplitsSequenceFileInputFormat[K >: Null <: WritableComparable[K], V >: Null]() extends FileInputFormat[K, V] {

  override 
  def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[K, V] = {
    new SplitsSequenceFileRecordReader[K,V]()
  }

  override
  protected def getFormatMinSplitSize(): Long =
    return SequenceFile.SYNC_INTERVAL

  class SplitsSequenceFileRecordReader[K >: Null <: WritableComparable[K], V >: Null]() extends RecordReader[K, V] {
    private var in: SequenceFile.Reader = null
    private var start: Long = 0L
    private var end: Long = 0L
    private var more: Boolean = true
    private var key: K = null
    private var value: V = null

    private var minKey: K = null
    private var maxKey: K = null

    override
    def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
      val conf = context.getConfiguration

      val fileSplit = split.asInstanceOf[FileSplit]
      val dataPath = fileSplit.getPath
      val keyBoundsPath = new Path(dataPath.getParent, SplitsMapFileOutputFormat.SPLITS_FILE)

      val fs = dataPath.getFileSystem(conf)

      val sequenceFile = SequenceFile.Reader.file(dataPath.makeQualified(fs.getUri, fs.getWorkingDirectory))
      this.in = new SequenceFile.Reader(conf, sequenceFile)
      this.end = fileSplit.getStart + fileSplit.getLength

      if (fileSplit.getStart > in.getPosition) {
        in.sync(fileSplit.getStart)                  // sync to start
      }

      this.start = in.getPosition
      more = start < end
    }

    override
    def nextKeyValue(): Boolean = {
      if (more) {
        val pos = in.getPosition
        key = in.next(key.asInstanceOf[Object]).asInstanceOf[K]
        
        if (key == null || (pos >= end && in.syncSeen)) {
          more = false
          key = null
          value = null
        } else {
          value = in.getCurrentValue(value).asInstanceOf[V]
        }
      }

      more
    }

    override
    def getCurrentKey(): K = key

    override
    def getCurrentValue(): V = value

    /**
      * Return the progress within the input split
      * @return 0.0 to 1.0 of the input byte range
      */
    override
    def getProgress(): Float = {
      if (end == start) {
        return 0.0f
      } else {
        return math.min(1.0f, (in.getPosition() - start) / (end - start).toFloat)
      }
    }

    override
    def close() { if(in != null) { in.close() } }
  }
}
