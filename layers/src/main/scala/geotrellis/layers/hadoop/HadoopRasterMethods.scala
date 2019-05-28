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

package geotrellis.layers.hadoop

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path


trait HadoopRasterMethods[T] extends MethodExtensions[T] {
  def write(path: Path, conf: Configuration): Unit
}

abstract class JpgHadoopWriteMethods(self: Jpg) extends HadoopRasterMethods[Jpg] {
  def write(path: Path, conf: Configuration): Unit =
    HdfsUtils.write(path, conf) { _.write(self.bytes) }
}

abstract class PngHadoopWriteMethods(self: Png) extends HadoopRasterMethods[Png] {
  def write(path: Path, conf: Configuration): Unit =
    HdfsUtils.write(path, conf) { _.write(self.bytes) }
}

abstract class GeoTiffHadoopWriteMethods[T <: CellGrid[Int]](self: GeoTiff[T]) extends HadoopRasterMethods[GeoTiff[T]] {
  def write(path: Path, conf: Configuration): Unit =
    HdfsUtils.write(path, conf) { new GeoTiffWriter(self, _).write() }
}
