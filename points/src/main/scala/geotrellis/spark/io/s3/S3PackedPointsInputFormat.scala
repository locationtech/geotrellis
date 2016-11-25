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

import geotrellis.spark.points.json._
import geotrellis.util.Filesystem

import io.pdal._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

import java.io.{BufferedOutputStream, File, FileOutputStream}

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class S3PackedPointsInputFormat extends S3InputFormat[String, Iterator[PackedPoints]] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = {
    val tmpDir = Filesystem.createDirectory()
    val s3Client = getS3Client(context)

    new S3RecordReader[String, Iterator[PackedPoints]](s3Client) {
      def read(key: String, bytes: Array[Byte]) = {
        // copy remote file into local tmp dir
        val localPath = new File(tmpDir, key)
        val bos = new BufferedOutputStream(new FileOutputStream(localPath))
        Stream.continually(bos.write(bytes))
        bos.close()

        val pipeline = Pipeline(fileToPipelineJson(localPath).toString)

        AnyRef.synchronized { pipeline.initialise() }
        AnyRef.synchronized { pipeline.execute }

        val pointViewIterator = AnyRef.synchronized { pipeline.pointViews() }
        // conversion to list to load everything into JVM memory
        val packedPoints = pointViewIterator.toList.map { pointView =>
          val packedPoint = AnyRef.synchronized {
            pointView.getPackedPointsWithMetadata(
              metadata = AnyRef.synchronized { pipeline.getMetadata() },
              schema = AnyRef.synchronized { pipeline.getSchema() }
            )
          }

          AnyRef.synchronized { pointView.dispose() }
          packedPoint
        }.toIterator

        val result = key -> packedPoints

        AnyRef.synchronized { pointViewIterator.dispose() }
        AnyRef.synchronized { pipeline.dispose() }
        localPath.delete()
        tmpDir.delete()

        result
      }
    }
  }
}
