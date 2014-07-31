/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.ingest

import geotrellis._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit
import org.apache.spark.Logging

class MetadataInputFormat extends FileInputFormat[String, Option[GeoTiff.Metadata]] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(
    split: InputSplit, 
    context: TaskAttemptContext): RecordReader[String, Option[GeoTiff.Metadata]] =
    new MetadataRecordReader

}

class MetadataRecordReader extends RecordReader[String, Option[GeoTiff.Metadata]] with Logging {
  private var file: String = null
  private var meta: Option[GeoTiff.Metadata] = null
  private var readFirstRecord: Boolean = false

  override def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val filePath = split.asInstanceOf[FileSplit].getPath()
    meta = GeoTiff.getMetadata(filePath, context.getConfiguration())
    file = filePath.toUri().toString()
  }

  override def getCurrentKey = file
  override def getCurrentValue = {
    readFirstRecord = true
    meta
  }
  override def getProgress = 1
  override def nextKeyValue = !readFirstRecord
  override def close = {}

}
