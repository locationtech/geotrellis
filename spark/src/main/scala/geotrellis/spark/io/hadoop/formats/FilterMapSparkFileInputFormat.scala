/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.hadoop.formats

import geotrellis.layers._
import geotrellis.layers.hadoop._
import geotrellis.layers.hadoop.formats._
import geotrellis.layers.index.MergeQueue
import geotrellis.spark.io.hadoop._

import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import scala.collection.JavaConverters._
import scala.reflect._



class FilterMapSparkFileInputFormat() extends FileInputFormat[BigIntWritable, BytesWritable] {
  var _filterDefinition: Option[FilterMapFileInputFormat.FilterDefinition] = None

  def createKey() = new BigIntWritable(Array[Byte](0))

  def createKey(index: BigInt) = new BigIntWritable(index.toByteArray)

  def createValue() = new BytesWritable

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

  /**
    * Produce list of files that overlap our query region.  This
    * function will be called on the driver.
    */
  override
  def listStatus(context: JobContext): java.util.List[FileStatus] = {
    val conf = context.getConfiguration
    val filterDefinition = getFilterDefinition(conf)

    val arr = filterDefinition
    val it = arr.iterator.buffered
    val dataFileStatus = super.listStatus(context)

    val possibleMatches =
      FilterMapFileInputFormat
        .mapFileRanges(dataFileStatus.asScala.map(_.getPath.getParent), conf)
        .filter { case (file, iMin, iMax) =>
          // both file ranges and query ranges are sorted, use in-sync traversal
          while (it.hasNext && it.head._2 < iMin) it.next
          if (it.hasNext) iMin <= it.head._2 && (iMax == -1 || it.head._1 <= iMax)
          else false
        }
        .map(_._1)
        .toSet

    dataFileStatus.asScala.filter(s => possibleMatches contains s.getPath.getParent).asJava
  }

  override
  def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[BigIntWritable, BytesWritable] =
    new FilterMapFileRecordReader(getFilterDefinition(context.getConfiguration))

  override
  protected def getFormatMinSplitSize(): Long =
    return SequenceFile.SYNC_INTERVAL

  /**
    * The map files are not meant to be split. Raster sequence files
    * are written such that data files should only be one large
    * block.
    */
  override
  def isSplitable(context: JobContext, filename: Path): Boolean =
    false

  class FilterMapFileRecordReader(filterDefinition: FilterMapFileInputFormat.FilterDefinition) extends RecordReader[BigIntWritable, BytesWritable] {
    private var mapFile: MapFile.Reader = null
    private var start: Long = 0L
    private var more: Boolean = true
    private var key: BigIntWritable = null
    private var value: BytesWritable = null

    private val ranges = filterDefinition
    private var currMinIndex: BigInt = BigInt(0)
    private var currMaxIndex: BigInt = BigInt(0)
    private var nextRangeIndex: Int = 0

    private var seek = false
    private var seekKey: BigIntWritable = null

    private def setNextIndexRange(index: BigInt = BigInt(0)): Boolean = {
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
            if(key == null || BigInt(key.getBytes) < BigInt(seekKey.getBytes)) {
              // We are seeking to the beginning of a new range.
              key = mapFile.getClosest(seekKey, nextValue).asInstanceOf[BigIntWritable]
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
            val nextKeyBytes: Array[Byte] = nextKey.getBytes.take(nextKey.getLength)

            if ((nextKeyBytes.length > 0) && (BigInt(nextKeyBytes) > currMaxIndex)) {
              // Must be out of current index range.
              if(nextRangeIndex < ranges.size) {
                if(!setNextIndexRange(BigInt(nextKeyBytes))) {
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
    def getCurrentKey(): BigIntWritable = key

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
