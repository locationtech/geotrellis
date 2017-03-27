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

package geotrellis.spark.raster

import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.spark.SparkContext

import java.io.DataOutputStream

trait HadoopWriteMethods[T] extends MethodExtensions[T] {
  def write(path: Path)(implicit sc: SparkContext): Unit = write(path, gzip = false)
  def write(path: Path, gzip: Boolean)(implicit sc: SparkContext): Unit = write(path, gzip, sc.hadoopConfiguration)
  def write(path: Path, conf: Configuration): Unit = write(path, gzip = false, conf)
  def write(path: Path, gzip: Boolean, conf: Configuration): Unit
}

object HadoopWriteMethods {
  def write(path: Path, gzip: Boolean, conf: Configuration)(dosWrite: DataOutputStream => Unit): Unit = {
    val fs = FileSystem.get(conf)

    val os =
      if (!gzip) {
        fs.create(path)
      } else {
        val factory = new CompressionCodecFactory(conf)
        val outputUri = new Path(s"${path.toUri.toString}.gz")

        val codec = factory.getCodec(outputUri)

        if (codec == null) {
          println(s"No codec found for $outputUri, writing without compression.")
          fs.create(path)
        } else {
          codec.createOutputStream(fs.create(outputUri))
        }
      }
    try {
      val dos = new DataOutputStream(os)
      try {
        dosWrite(dos)
      } finally {
        dos.close
      }
    } finally {
      os.close
    }
  }
}
