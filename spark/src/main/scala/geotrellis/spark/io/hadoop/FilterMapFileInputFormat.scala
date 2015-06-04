package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.io._
import org.apache.hadoop.fs._
import org.apache.hadoop.conf._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import scala.collection.JavaConversions._

object FilterMapFileInputFormat {
  // Define some key names for Hadoop configuration
  val SPLITS_FILE_PATH = "splits_file_path"
  val FILTER_INFO_KEY = "geotrellis.spark.io.hadoop.filterinfo"

  type FilterDefinition[K] = (Seq[KeyBounds[K]], Array[(Long, Long)])
}

abstract class FilterMapFileInputFormat[K: Boundable, KW >: Null <: WritableComparable[KW] with IndexedKeyWritable[K], V >: Null <: Writable]() extends FileInputFormat[KW, V] {
  var _filterDefinition: Option[FilterMapFileInputFormat.FilterDefinition[K]] = None

  def createKey(): KW
  def createKey(index: Long): KW

  def createValue(): V

  def getFilterDefinition(conf: Configuration): FilterMapFileInputFormat.FilterDefinition[K] =
    _filterDefinition match {
      case Some(fd) => fd
      case None =>
        val r = conf.getSerialized[FilterMapFileInputFormat.FilterDefinition[K]](FilterMapFileInputFormat.FILTER_INFO_KEY)
        _filterDefinition = Some(r)
        val compressedRanges = MergeQueue(r._2).sortBy(_._1).toArray
        r._1 -> compressedRanges // Index ranges MUST be sorted, the reader will NOT do it.
    }

  override
  def listStatus(context: JobContext): java.util.List[FileStatus] = {
    val conf = context.getConfiguration
    val filterDefinition = getFilterDefinition(conf)

    // Read the index, figure out if this file has any of the desired values.
    def fileStatusFilter(fileStatus: FileStatus): Boolean = {
      val indexPath = new Path(fileStatus.getPath.getParent, "index")
      val fs = indexPath.getFileSystem(conf)
      val in = new SequenceFile.Reader(conf, SequenceFile.Reader.file(indexPath))

      val minKey = createKey
      val maxKey = createKey

      try {
        val dummyLongWritable = new LongWritable

        in.next(minKey, dummyLongWritable)
        while(in.next(maxKey, dummyLongWritable)) { }
      } finally {
        in.close()
      }

      val iMin = minKey.index
      val iMax = maxKey.index

      var i = 0
      val arr = filterDefinition._2
      val len = arr.size
      while(i < len) {
        val (min, max) = arr(i)

        if(iMin < min) {
          if(iMax < min) { return false }
          else { return true }
        }

        if(iMin < max) { return true }

        i += 1
      }

      false
    }

    super.listStatus(context).filter(fileStatusFilter)
  }

  override
  def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[KW, V] =
    new FilterMapFileRecordReader(getFilterDefinition(context.getConfiguration))

  override
  protected def getFormatMinSplitSize(): Long =
    return SequenceFile.SYNC_INTERVAL

  /** The map files are not meant to be split. Raster sequence files
    * are written such that data files should only be one block large */
  override
  def isSplitable(context: JobContext, filename: Path): Boolean =
    false

  class FilterMapFileRecordReader(filterDefinition: FilterMapFileInputFormat.FilterDefinition[K]) extends RecordReader[KW, V] {
    private var mapFile: MapFile.Reader = null
    private var start: Long = 0L
    private var more: Boolean = true
    private var key: KW = null
    private var value: V = null

    private val ranges = filterDefinition._2
    private var currMinIndex: Long = 0L
    private var currMaxIndex: Long = 0L
    private var nextRangeIndex: Int = 0

    private def setNextIndexRange(index: Long = 0L): Boolean = {
      if(nextRangeIndex >= ranges.size) {
        false
      } else {
        // Find next index
        val (minIndex, maxIndex) = ranges(nextRangeIndex)
        nextRangeIndex += 1

        if(index > maxIndex) {
          setNextIndexRange(index)
        } else {
          currMinIndex = minIndex
          currMaxIndex = maxIndex

          // Seek to the beginning of this index range
          val seekKey =
            if(minIndex < index) {
              createKey(index)
            } else {
              createKey(minIndex)
            }

          // Need to use "getClosest" with before = true
          // because if we use seek, it will seek to the correct key
          // and then calling next will get the key after the correct key.
          // Since there's no seek(before = true), we need to call get,
          // and read in a dummy value
          mapFile.getClosest(seekKey, createValue, true)
          true
        }
      }
    }

    override
    def initialize(split: InputSplit, context: TaskAttemptContext): Unit = {
      val conf = context.getConfiguration

      val fileSplit = split.asInstanceOf[FileSplit]
      val dataPath = fileSplit.getPath
      val mapFilePath = dataPath.getParent

      val fs = dataPath.getFileSystem(conf)

      this.mapFile = new MapFile.Reader(mapFilePath, conf)
      setNextIndexRange()
    }

    override
    def nextKeyValue(): Boolean = {
      if (more) {
        val nextKey = createKey()
        val nextValue = createValue()
        var break = false
        while(!break) {
          if(!mapFile.next(nextKey, nextValue)) {
            break = true
            more = false
            key = null
            value = null
          } else {
            if (nextKey.index > currMaxIndex) {
              // Must be out of current index range.
              if(nextRangeIndex < ranges.size) {
                if(!setNextIndexRange(nextKey.index)) {
                  break = true
                  more = false
                  key = null
                  value = null
                }
              } else {
                break = true
                more = false
                key = null
                value = null
              }
            } else {
              if (filterDefinition._1.includeKey(nextKey.key)) {
                break = true
                key = nextKey
                value = nextValue
              }
            }
          }
        }
      }

      more
    }

    override
    def getCurrentKey(): KW = key

    override
    def getCurrentValue(): V = value

    /**
      * Return the progress within the input split
      * @return 0.0 to 1.0 of the input byte range
      */
    override
    def getProgress(): Float = 0.0f // Not sure how to measure this, or if we need to.

    override
    def close() { if(mapFile != null) { mapFile.close() } }
  }
}
