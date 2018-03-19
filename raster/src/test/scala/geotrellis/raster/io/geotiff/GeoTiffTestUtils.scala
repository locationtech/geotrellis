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

package geotrellis.raster.io.geotiff

import java.io.File

import org.scalatest._

object GeoTiffTestUtils {
  val testDirPath = "raster/data/geotiff-test-files"
}

trait GeoTiffTestUtils extends Matchers {
  import GeoTiffTestUtils._

  def geoTiffPath(name: String): String = {
    val path = s"$testDirPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  def baseDataPath = "raster/data"

  val Epsilon = 1e-9

  var writtenFiles = Vector[String]()

  protected def addToPurge(path: String) = synchronized {
    writtenFiles = writtenFiles :+ path
  }

  protected def purge = writtenFiles foreach { path =>
    val file = new File(path)
    if (file.exists()) file.delete()
  }
}
