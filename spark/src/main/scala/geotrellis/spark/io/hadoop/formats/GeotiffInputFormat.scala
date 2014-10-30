/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector.Extent
import geotrellis.proj4._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FSDataInputStream

import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit

import java.nio.ByteBuffer

class GeotiffInputFormat extends FileInputFormat[ProjectedExtent, Tile] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(
    split: InputSplit,
    context: TaskAttemptContext): RecordReader[ProjectedExtent, Tile] = new GeotiffRecordReader

}

class GeotiffRecordReader extends RecordReader[ProjectedExtent, Tile] {
  private var tup: (ProjectedExtent, Tile) = null
  private var hasNext: Boolean = true

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val path = split.asInstanceOf[FileSplit].getPath()
    val conf = context.getConfiguration()
    val bytes = HdfsUtils.readBytes(path, conf)

    val (tile, extent, crs) =
      GeoTiffReader(bytes).read().imageDirectories.head.toRaster

    tup = (ProjectedExtent(extent, crs), tile)
  }

  def close = {}
  def getCurrentKey = tup._1
  def getCurrentValue = { hasNext = false ; tup._2 }
  def getProgress = 1
  def nextKeyValue = hasNext
}
