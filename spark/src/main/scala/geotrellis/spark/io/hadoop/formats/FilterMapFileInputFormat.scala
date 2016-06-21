package geotrellis.spark.io.hadoop.formats

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.MergeQueue
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import scala.collection.JavaConversions._
import scala.reflect._

object FilterMapFileInputFormat {
  // Define some key names for Hadoop configuration
  val FILTER_INFO_KEY = "geotrellis.spark.io.hadoop.filterinfo"

  type FilterDefinition = Array[(Long, Long)]
}

class FilterMapFileInputFormat() extends FileInputFormat[LongWritable, BytesWritable] {
  var _filterDefinition: Option[FilterMapFileInputFormat.FilterDefinition] = None

  def createKey() = new LongWritable()

  def createKey(index: Long) = new LongWritable(index)

  def createValue() = new BytesWritable()

  def getFilterDefinition(conf: Configuration): FilterMapFileInputFormat.FilterDefinition =
    _filterDefinition match {
      case Some(fd) => fd
      case None =>
        val r = conf.getSerialized[FilterMapFileInputFormat.FilterDefinition](FilterMapFileInputFormat.FILTER_INFO_KEY)
        // Index ranges MUST be sorted, the reader will NOT do it.
        val compressedRanges = MergeQueue(r).sortBy(_._1).toArray
        _filterDefinition = Some(compressedRanges)
        compressedRanges
    }

  override
  def listStatus(context: JobContext): java.util.List[FileStatus] = {
    val conf = context.getConfiguration
    val filterDefinition = getFilterDefinition(conf)

    // Read the index, figure out if this file has any of the desired values.
    def fileStatusFilter(fileStatus: FileStatus): Boolean = {
      val indexPath = new Path(fileStatus.getPath.getParent, "index")
      val in = new SequenceFile.Reader(conf, SequenceFile.Reader.file(indexPath))

      val minKey = createKey
      val maxKey = createKey

      try {
        in.next(minKey)
        while(in.next(maxKey)) { }
      } finally {
        in.close()
      }

      val iMin = minKey.get
      val iMax = maxKey.get

      var i = 0
      val arr = filterDefinition
      val len = arr.length


      while(i < len) {
        val (min, max) = arr(i)

        // max index in this file is smaller than what we're looking for, abort (assert: ranges are sorted in asc)
        if (iMax < min) return false

        // check if this query ranges overlap at all with min/max index from the sequence file
        if (iMin <= max && min <= iMax) return true
        i += 1
      }

      false
    }

    super.listStatus(context).filter(fileStatusFilter)
  }

  override
  def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[LongWritable, BytesWritable] =
    new FilterMapFileRecordReader(getFilterDefinition(context.getConfiguration))

  override
  protected def getFormatMinSplitSize(): Long =
    return SequenceFile.SYNC_INTERVAL

  /** The map files are not meant to be split. Raster sequence files
    * are written such that data files should only be one block large */
  override
  def isSplitable(context: JobContext, filename: Path): Boolean =
    false

  class FilterMapFileRecordReader(filterDefinition: FilterMapFileInputFormat.FilterDefinition) extends RecordReader[LongWritable, BytesWritable] {
    private var mapFile: MapFile.Reader = null
    private var start: Long = 0L
    private var more: Boolean = true
    private var key: LongWritable = null
    private var value: BytesWritable = null

    private val ranges = filterDefinition
    private var currMinIndex: Long = 0L
    private var currMaxIndex: Long = 0L
    private var nextRangeIndex: Int = 0

    private var seek = false
    private var seekKey: LongWritable = null

    private def setNextIndexRange(index: Long = 0L): Boolean = {
      if(nextRangeIndex >= ranges.length) {
        false
      } else {
        // Find next index
        val (minIndexInRange, maxIndexInRange) = ranges(nextRangeIndex)
        nextRangeIndex += 1

        if(index > maxIndexInRange) {
          setNextIndexRange(index)
        } else {

          currMinIndex = minIndexInRange
          currMaxIndex = maxIndexInRange

          // Seek to the beginning of this index range
          seekKey =
            if(minIndexInRange < index) {
              createKey(index)
            } else {
              createKey(minIndexInRange)
            }

          seek = true
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
          if(seek) {
            seek = false
            if(key == null || key.get < seekKey.get) {
              // We are seeking to the beginning of a new range.
              key = mapFile.getClosest(seekKey, nextValue).asInstanceOf[LongWritable]
              if(key == null) {
                break = true
                more = false
                value = null
              } else {
                value = nextValue
              }
            } // else the previously read key is within this new range
          } else {
            // We are getting the next key in a range currently being explored.
            if(!mapFile.next(nextKey, nextValue)) {
              break = true
              more = false
              key = null
              value = null
            }
          }

          if(!break) {
            if (nextKey.get > currMaxIndex) {
              // Must be out of current index range.
              if(nextRangeIndex < ranges.size) {
                if(!setNextIndexRange(nextKey.get)) {
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
              break = true
              key = nextKey
              value = nextValue
            }
          }
        }
      }

      more
    }

    override
    def getCurrentKey(): LongWritable = key

    override
    def getCurrentValue(): BytesWritable = value

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
