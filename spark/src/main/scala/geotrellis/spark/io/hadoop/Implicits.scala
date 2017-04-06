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

package geotrellis.spark.io.hadoop

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.render.{Jpg, Png}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark._
import org.apache.spark.rdd._

object Implicits extends Implicits

trait Implicits {
  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods
  implicit class withSaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) extends SaveBytesToHadoopMethods[K](rdd)
  implicit class withSaveToHadoopMethods[K,V](rdd: RDD[(K,V)]) extends SaveToHadoopMethods[K, V](rdd)

  implicit class withJpgHadoopWriteMethods(val self: Jpg) extends HadoopRasterMethods[Jpg] {
    def write(path: Path, conf: Configuration): Unit =
      HdfsUtils.write(path, conf) { _.write(self.bytes) }
  }

  implicit class withPngHadoopWriteMethods(val self: Png) extends HadoopRasterMethods[Png] {
    def write(path: Path, conf: Configuration): Unit =
      HdfsUtils.write(path, conf) { _.write(self.bytes) }
  }

  implicit class withGeoTiffHadoopWriteMethods[T <: CellGrid](val self: GeoTiff[T]) extends HadoopRasterMethods[GeoTiff[T]] {
    def write(path: Path, conf: Configuration): Unit =
      HdfsUtils.write(path, conf) { new GeoTiffWriter(self, _).write() }
  }
}
