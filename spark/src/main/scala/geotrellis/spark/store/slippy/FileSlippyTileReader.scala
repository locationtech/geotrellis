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

package geotrellis.spark.store.slippy

import geotrellis.tiling.SpatialKey
import geotrellis.spark._
import geotrellis.util.Filesystem

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import org.apache.spark._
import org.apache.spark.rdd._

import java.io._
import scala.collection.JavaConverters._

class FileSlippyTileReader[T](uri: String, extensions: Seq[String] = Seq())(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  private def listFiles(path: String): Seq[File] =
    listFiles(new File(path))

  private def listFiles(file: File): Seq[File] =
    if(extensions.isEmpty) { FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).asScala.toSeq }
    else { FileUtils.listFiles(file, new SuffixFileFilter(extensions.asJava), TrueFileFilter.INSTANCE).asScala.toSeq }

  def read(zoom: Int, key: SpatialKey): T = {
    val dir = new File(uri, s"$zoom/${key.col}/")

    val lFromBytes = fromBytes
    listFiles(dir).filter { f => f.getName.startsWith(s"${key.row}") } match {
      case Seq() => throw new FileNotFoundException(s"${dir}/${key.row}*")
      case Seq(tilePath) => lFromBytes(key, Filesystem.slurp(tilePath.getAbsolutePath))
      case _ => throw new IllegalArgumentException(s"More than one file matches path ${dir}/${key.row}*")
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val paths = {
      listFiles(new File(uri, zoom.toString).getPath)
        .flatMap { file =>
          val path = file.getAbsolutePath
          path match {
            case TilePath(x, y) => Some((SpatialKey(x.toInt, y.toInt), path))
            case _ => None
          }
        }
    }

    val lFromBytes = fromBytes
    val numPartitions = math.min(paths.size, math.max(paths.size / 10, 50)).toInt
    sc.parallelize(paths.toSeq)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        partition.map { case (key, path) => (key, lFromBytes(key, Filesystem.slurp(path))) }
      }, true)
  }
}
