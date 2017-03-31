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

import geotrellis.util.MethodExtensions

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.spark.SparkContext
import java.io.{DataInputStream, DataOutputStream}


trait HadoopRasterMethods[T] extends MethodExtensions[T] {
  def write(path: Path)(implicit sc: SparkContext): Unit = write(path, sc.hadoopConfiguration)
  def write(path: Path, conf: Configuration): Unit
}

object HadoopRasterMethods {
  def write(path: Path, conf: Configuration)(dosWrite: DataOutputStream => Unit): Unit = {
    val fs = FileSystem.get(conf)

    val os = {
      val factory = new CompressionCodecFactory(conf)
      val codec = factory.getCodec(path)

      if (codec == null) {
        println(s"No codec found for $path, writing without compression.")
        fs.create(path)
      } else {
        codec.createOutputStream(fs.create(path))
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

  def read[T](path: Path, conf: Configuration)(disRead: DataInputStream => T): T = {
    val fs = FileSystem.get(conf)

    val is = {
      val factory = new CompressionCodecFactory(conf)
      val codec = factory.getCodec(path)

      if (codec == null) {
        println(s"No codec found for $path, reading without compression.")
        fs.open(path)
      } else {
        codec.createInputStream(fs.open(path))
      }
    }
    try {
      val dis = new DataInputStream(is)
      try {
        disRead(dis)
      } finally {
        dis.close
      }
    } finally {
      is.close
    }
  }
}
