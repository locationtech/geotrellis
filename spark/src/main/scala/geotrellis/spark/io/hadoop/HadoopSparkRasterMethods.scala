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
import geotrellis.layers.hadoop._
import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext


trait HadoopSparkRasterMethods[T] extends HadoopRasterMethods[T] {
  def write(path: Path)(implicit sc: SparkContext): Unit = write(path, sc.hadoopConfiguration)
}

abstract class JpgHadoopSparkWriteMethods(self: Jpg) extends JpgHadoopWriteMethods(self) with HadoopSparkRasterMethods[Jpg]

abstract class PngHadoopSparkWriteMethods(self: Png) extends PngHadoopWriteMethods(self) with HadoopSparkRasterMethods[Png]

abstract class GeoTiffHadoopSparkWriteMethods[T <: CellGrid[Int]](self: GeoTiff[T]) extends GeoTiffHadoopWriteMethods[T](self) with HadoopSparkRasterMethods[GeoTiff[T]]
