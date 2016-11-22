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

package geotrellis.spark.io.s3

import io.pdal._

import geotrellis.spark.io.pdal.json._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

import java.io.{BufferedOutputStream, File, FileOutputStream}

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class PackedPointsS3InputFormat extends S3InputFormat[String, PackedPoints] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = {
    val tmpDir = new File(System.getProperty("java.io.tmpdir"), context.getTaskAttemptID.toString)
    val s3Client = getS3Client(context)
    new S3RecordReader[String, PackedPoints](s3Client) {
      def read(key: String, bytes: Array[Byte]) = {
        // copy remote file into local tmp dir
        tmpDir.mkdirs()
        val localPath = new File(tmpDir, key)
        val bos = new BufferedOutputStream(new FileOutputStream(localPath))
        Stream.continually(bos.write(bytes))
        bos.close()

        val pipeline = Pipeline(fileToPipelineJson(localPath).toString)

        pipeline.execute

        val pointViewIterator = pipeline.pointViews()
        val pointView = pointViewIterator.next()

        val packedPoint = pointView.getPackedPointsWithMetadata(
          metadata = pipeline.getMetadata(),
          schema   = pipeline.getSchema()
        )

        val result = key -> packedPoint

        pointView.dispose()
        pointViewIterator.dispose()
        pipeline.dispose()
        localPath.delete()
        result
      }
    }
  }
}
