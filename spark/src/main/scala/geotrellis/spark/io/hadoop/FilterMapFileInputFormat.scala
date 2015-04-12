package geotrellis.spark.io.hadoop

import org.apache.hadoop.io._
import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

object FilterMapFileInputFormat {
  // Define some key names for Hadoop configuration
  val SPLITS_FILE_PATH = "splits_file_path"
}

abstract class FilterMapFileInputFormat[K >: Null <: WritableComparable[K], V >: Null <: Writable]() extends FileInputFormat[K, V] {
  def createKey(): K
  def createValue(): V

  override 
  def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[K, V] = {
    new SplitsSequenceFileRecordReader()
  }

  override
  protected def getFormatMinSplitSize(): Long =
    return SequenceFile.SYNC_INTERVAL

  /** The map files are not meant to be split. Raster sequence files
    * are written such that data files should only be one block large */
  override
  def isSplitable(context: JobContext, filename: Path): Boolean =
    false

  class SplitsSequenceFileRecordReader() extends RecordReader[K, V] {
    private var mapFile: MapFile.Reader = null
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
      val mapFilePath = dataPath.getParent

      val fs = dataPath.getFileSystem(conf)

      this.mapFile = new MapFile.Reader(mapFilePath, conf)
      this.end = fileSplit.getStart + fileSplit.getLength
    }

    override
    def nextKeyValue(): Boolean = {
      if (more) {
        val nextKey = createKey()
        val nextValue = createValue()
        if(!mapFile.next(nextKey, nextValue)) {
          more = false
          key = null
          value = null
        } else {
          key = nextKey
          value = nextValue
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
      // if (end == start) {
      //   return 0.0f
      // } else {
//        return math.min(1.0f, (mapFile.getPosition() - start) / (end - start).toFloat)
        0.0f
//      }
    }

    override
    def close() { if(mapFile != null) { mapFile.close() } }
  }
}
