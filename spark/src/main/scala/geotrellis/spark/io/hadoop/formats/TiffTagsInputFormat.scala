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

import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.layers.hadoop.HdfsRangeReader
import geotrellis.spark.io.hadoop._
import geotrellis.util.StreamingByteReader

import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

/**
 * This class extends [[FileInputFormat]] and is used to create RDDs of TiffTags.
 * from files on Hdfs.
 */
class TiffTagsInputFormat extends FileInputFormat[Path, TiffTags] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  /**
   * Creates a RecordReader that can be used to read [[TiffTags]] from Hdfs.
   *
   * @param split: The [[InputSplit]] that contains the data to make the RDD.
   * @param context: The [[TaskAttemptContext]] of the InputSplit.
   *
   * @return A [[RecordReader]] that can read [[TiffTags]] of GeoTiffs from Hdfs.
   */
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new RecordReader[Path, TiffTags] {
      private var tup: (Path, TiffTags) = null
      private var hasNext: Boolean = true

      def initialize(split: InputSplit, context: TaskAttemptContext) = {
        val path = split.asInstanceOf[FileSplit].getPath()
        val conf = context.getConfiguration()
        val byteReader = StreamingByteReader(HdfsRangeReader(path, conf))

        tup = (path, TiffTagsReader.read(byteReader))
      }

      def close = {}
      def getCurrentKey = tup._1
      def getCurrentValue = { hasNext = false ; tup._2 }
      def getProgress = 1
      def nextKeyValue = hasNext
    }
}
