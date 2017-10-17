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

package geotrellis.spark.io.file.geotiff

import geotrellis.raster.testkit._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.testfiles._

import java.net.URI
import org.scalatest._

class SinglebandGeoTiffRDDLayerReaderSpec extends FunSpec
  with Matchers
  with RasterMatchers
  with TestEnvironment
  with TestFiles {

  describe("HadoopGeoTiffRDD") {

    it("first") {
      val path = new URI("file:///Users/daunnc/subversions/git/github/pomadchin/geotrellis-chatta-demo/service/geotrellis/data/arg_wm/")
      val layer = FileSinglebandGeoTiffRDDLayerReader.fetchSingleband(path)

      println(s"layer.rdd.length: ${layer.rdd.count}")

    }

  }
}
