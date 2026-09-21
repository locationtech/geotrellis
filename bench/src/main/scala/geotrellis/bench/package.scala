/*
 * Copyright 2019 Azavea
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

package geotrellis

import geotrellis.raster.io.geotiff.{MultibandGeoTiff, SinglebandGeoTiff}
import scala.reflect.ClassTag

package object bench {
  /** Sugar for building arrays using a per-cell init function */
  def init[A: ClassTag](size: Int)(init: => A): Array[A] = {
    val data = Array.ofDim[A](size)
    for (i <- 0 until size) data(i) = init
    data
  }

  def resourceBytes(name: String): Array[Byte] = {
    val is = getClass.getResourceAsStream("/" + name)
    require(is != null, s"benchmark resource not found on the classpath: /$name")
    try {
      val out = new java.io.ByteArrayOutputStream()
      val buf = new Array[Byte](8192)
      var n = is.read(buf)
      while (n >= 0) { out.write(buf, 0, n); n = is.read(buf) }
      out.toByteArray
    } finally is.close()
  }

  def readSinglebandGeoTiff(name: String): SinglebandGeoTiff =
    SinglebandGeoTiff(resourceBytes(name))

  def readMultibandGeoTiff(name: String): MultibandGeoTiff =
    MultibandGeoTiff(resourceBytes(name))
}
