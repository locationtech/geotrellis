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

import geotrellis.spark.pdal.{PackedPoints, SizedDimType}

import io.pdal.Pipeline
import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.{BufferedOutputStream, File, FileOutputStream}

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class PackedPointsInputFormat extends FileInputFormat[Path, Array[Byte]] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Path, PackedPoints] = {
    val tmpDir = new File(System.getProperty("java.io.tmpdir"), context.getTaskAttemptID.toString)

    new BinaryFileRecordReader({ bytes =>
      val remotePath = split.asInstanceOf[FileSplit].getPath

      // copy remote file into local tmp dir
      val localPath = new File(tmpDir, remotePath.getName)
      val bos = new BufferedOutputStream(new FileOutputStream(localPath))
      Stream.continually(bos.write(bytes))
      bos.close()

      // "spatialreference":"EPSG:$code" ? // parametered
      val pipeline = Pipeline(JsObject(
        "filename" -> localPath.getCanonicalPath.toJson
      ).toString)

      // Bad json exception handling
      if(pipeline.validate()) pipeline.execute else throw Exception

      // right now we read a single file, that means we have a single point view ? // double check that information
      val pointViewIterator = pipeline.pointViews()
      if(!pointViewIterator.hasNext) throw Exception
      val pointView = pointViewIterator.next()

      // ok we have packed points, what's next, how to unpack them? we need to save all dims and thier shifts
      val layout = pointView.layout
      val packedPoint = PackedPoints(pointView.getPackedPoints, SizedDimType.asMap(layout))

      val result = split.asInstanceOf[FileSplit].getPath -> packedPoint

      // free c memory
      layout.dispose()
      pointView.dispose()
      pointViewIterator.dispose()
      pipeline.dispose()
      result
    })
  }
}
