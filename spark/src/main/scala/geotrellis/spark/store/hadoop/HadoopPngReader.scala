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

import geotrellis.raster.render.Png
import geotrellis.layers.hadoop.HdfsUtils

import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext


object HadoopPngReader {
  def read(path: Path)(implicit sc: SparkContext): Png = read(path, sc.hadoopConfiguration)
  def read(path: Path, conf: Configuration): Png = HdfsUtils.read(path, conf) { is => Png(IOUtils.toByteArray(is)) }
}
