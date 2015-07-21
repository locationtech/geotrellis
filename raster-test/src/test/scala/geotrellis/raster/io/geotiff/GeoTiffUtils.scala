/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

trait GeoTiffTestUtils extends Matchers {
  def geoTiffPath(name: String): String = {
    initializeTestFiles
    s"raster-test/data/geotiff-test-files/$name"
  }

  def baseDataPath = "raster-test/data"

  val Epsilon = 1e-9

  var writtenFiles = Vector[String]()

  def initializeTestFiles(): Unit = {
    if(!new File("raster-test/data/geotiff-test-files").exists) {
      println("UNCOMPRESSING TEST FILES")
      ZipArchive.unZip("raster-test/data/geotiff-test-files.zip", "raster-test/data")
    }
  }  

  protected def addToPurge(path: String) = synchronized {
    writtenFiles = writtenFiles :+ path
  }

  protected def purge = writtenFiles foreach { path =>
    val file = new File(path)
    if (file.exists()) file.delete()
  }

}
