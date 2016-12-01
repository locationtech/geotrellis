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

import geotrellis.spark.pointcloud.json._
import geotrellis.util.Filesystem

import io.pdal._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import java.io.{BufferedOutputStream, File, FileOutputStream}

case class PointCloudHeader(fileName: Path, metadata: String, schema: String)

object PointCloudInputFormat {
  final val POINTCLOUD_TMP_DIR = "POINTCLOUD_TMP_DIR"

  def setTmpDir(conf: Configuration, dir: String): Unit =
    conf.set(POINTCLOUD_TMP_DIR, dir)

  def getTmpDir(job: JobContext): String =
    job.getConfiguration.get(POINTCLOUD_TMP_DIR)
}

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class PointCloudInputFormat extends FileInputFormat[PointCloudHeader, Iterator[PointCloud]] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[PointCloudHeader, Iterator[PointCloud]] = {
    val tmpDir = {
      val dir = PointCloudInputFormat.getTmpDir(context)
      if(dir == null) Filesystem.createDirectory()
      else Filesystem.createDirectory(dir)
    }

    new BinaryFileRecordReader({ bytes =>
      val remotePath = split.asInstanceOf[FileSplit].getPath

      // copy remote file into local tmp dir
      val localPath = new File(tmpDir, remotePath.getName)
      val bos = new BufferedOutputStream(new FileOutputStream(localPath))
      Stream.continually(bos.write(bytes))
      bos.close()

      val pipeline = Pipeline(fileToPipelineJson(localPath).toString)

      // PDAL itself is not threadsafe
      AnyRef.synchronized { pipeline.execute }

      val pointViewIterator = pipeline.getPointViews()
      // conversion to list to load everything into JVM memory
      val pointClouds =
        pointViewIterator.toList.map { pointView =>
          val pointCloud =
            pointView.getPointCloud

          pointView.dispose()
          pointCloud
        }.iterator

      val header =
        PointCloudHeader(
          split.asInstanceOf[FileSplit].getPath,
          pipeline.getMetadata(),
          pipeline.getSchema()
        )

      val result =  (header, pointClouds)

      pointViewIterator.dispose()
      pipeline.dispose()
      localPath.delete()
      tmpDir.delete()

      result
    })
  }
}
