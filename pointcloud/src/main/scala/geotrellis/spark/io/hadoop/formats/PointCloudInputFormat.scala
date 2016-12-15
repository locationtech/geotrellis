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

import geotrellis.spark.io.hadoop._
import geotrellis.spark.pointcloud.json._
import geotrellis.util.Filesystem
import geotrellis.vector.Extent

import io.pdal._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._
import spray.json._

import java.io.{BufferedOutputStream, File, FileOutputStream}
import scala.collection.JavaConversions._

object PointCloudInputFormat {
  final val POINTCLOUD_TMP_DIR = "POINTCLOUD_TMP_DIR"
  final val POINTCLOUD_FILTER_EXTENT = "POINTCLOUD_FILTER_EXTENT"
  final val POINTCLOUD_DIM_TYPES = "POINTCLOUD_DIM_TYPES"
  final val POINTCLOUD_TARGET_CRS = "POINTCLOUD_TARGET_CRS"
  final val POINTCLOUD_ADDITIONAL_STEPS ="POINTCLOUD_ADDITIONAL_STEPS"

  def setTmpDir(conf: Configuration, dir: String): Unit =
    conf.set(POINTCLOUD_TMP_DIR, dir)

  def getTmpDir(job: JobContext): String =
    job.getConfiguration.get(POINTCLOUD_TMP_DIR)

  def setFilterExtent(conf: Configuration, extent: Extent): Unit =
    conf.setSerialized(POINTCLOUD_FILTER_EXTENT, extent)

  def getFilterExtent(job: JobContext): Option[Extent] =
    job.getConfiguration.getSerializedOption[Extent](POINTCLOUD_FILTER_EXTENT)

  def setDimTypes(conf: Configuration, dimTypes: Iterable[String]): Unit =
    conf.set(POINTCLOUD_DIM_TYPES, dimTypes.mkString(";"))

  def getDimTypes(job: JobContext): Option[Array[String]] = {
    val s = job.getConfiguration.get(POINTCLOUD_DIM_TYPES)
    if(s != null) { Some(s.split(";")) }
    else { None }
  }

  // Be careful, metadata contained in PointCloudHeader won't be reprojected
  def setTargetCrs(conf: Configuration, targetCrs: String): Unit =
    conf.set(POINTCLOUD_TARGET_CRS, targetCrs)

  def getTargetCrs(job: JobContext): Option[String] = {
    val s = job.getConfiguration.get(POINTCLOUD_TARGET_CRS)
    if(s != null) { Some(s) }
    else { None }
  }

  def setAdditionalPipelineSteps(conf: Configuration, steps: Seq[JsObject]): Unit =
    conf.setSerialized(POINTCLOUD_ADDITIONAL_STEPS, steps)

  def getAdditionalPipelineSteps(job: JobContext): Seq[JsObject] =
    job.getConfiguration.getSerializedOption[Seq[JsObject]](POINTCLOUD_TARGET_CRS).toSeq.flatten
}

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class PointCloudInputFormat extends FileInputFormat[HadoopPointCloudHeader, Iterator[PointCloud]] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[HadoopPointCloudHeader, Iterator[PointCloud]] = {

    val tmpDir = {
      val dir = PointCloudInputFormat.getTmpDir(context)
      if(dir == null) Filesystem.createDirectory()
      else Filesystem.createDirectory(dir)
    }

    val dimTypeStrings = PointCloudInputFormat.getDimTypes(context)

    new BinaryFileRecordReader({ bytes =>
      val remotePath = split.asInstanceOf[FileSplit].getPath

      // copy remote file into local tmp dir
      val localPath = new File(tmpDir, remotePath.getName)
      val bos = new BufferedOutputStream(new FileOutputStream(localPath))
      Stream.continually(bos.write(bytes))
      bos.close()

      try {
        val pipeline =
          Pipeline(
            getPipelineJson(
              localPath,
              PointCloudInputFormat.getTargetCrs(context),
              PointCloudInputFormat.getAdditionalPipelineSteps(context)
            ).compactPrint
          )

        // PDAL itself is not threadsafe
        AnyRef.synchronized { pipeline.execute }

        val header =
          HadoopPointCloudHeader(
            split.asInstanceOf[FileSplit].getPath,
            pipeline.getMetadata(),
            pipeline.getSchema()
          )

        // If a filter extent is set, don't actually load points.
        val (pointViewIterator, disposeIterator): (Iterator[PointView], () => Unit) =
          PointCloudInputFormat.getFilterExtent(context) match {
            case Some(filterExtent) =>
              if(header.extent3D.toExtent.intersects(filterExtent)) {
                val pvi = pipeline.getPointViews()
                (pvi, pvi.dispose _)
              } else {
                (Iterator.empty, () => ())
              }
            case None =>
              val pvi = pipeline.getPointViews()
              (pvi, pvi.dispose _)
          }

        // conversion to list to load everything into JVM memory
        val pointClouds = pointViewIterator.toList.map { pointView =>
          val pointCloud =
            dimTypeStrings match {
              case Some(ss) =>
                pointView.getPointCloud(dims = ss.map(pointView.findDimType))
              case None =>
                pointView.getPointCloud()
            }

          pointView.dispose()
          pointCloud
        }.iterator

        val result = (header, pointClouds)

        disposeIterator()
        pipeline.dispose()

        result
      } finally {
        localPath.delete()
        tmpDir.delete()
      }
    })
  }
}
