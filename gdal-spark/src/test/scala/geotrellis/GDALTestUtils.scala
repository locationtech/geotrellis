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

import java.io.File

object GDALTestUtils {
  def gdalGeoTiffPath(name: String): String = {
    def baseDataPath = "../gdal/src/test/resources"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  def sparkGeoTiffPath(name: String): String = {
    def baseDataPath = "../spark/src/test/resources"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }
}
