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

package geotrellis.spark.io.cog3

import java.net.URI

import geotrellis.proj4.WebMercator
import geotrellis.raster.testkit._
import geotrellis.spark.{LayerId, SpatialKey}
import geotrellis.spark.io._
import geotrellis.spark.io.cog3.GeoTiffLayerMetadata.IndexRDD
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod, ZCurveKeyIndexMethod}
import geotrellis.spark.testkit._
import geotrellis.spark.tiling.ZoomedLayoutScheme
import org.apache.spark.rdd.RDD
import org.scalatest._

class IndexedGeoTiffLayerSpec extends FunSpec
  with Matchers
  with RasterMatchers
  with TestEnvironment
  with TestFiles {

  describe("HadoopGeoTiffRDD") {

    val keyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod

    it("first") {
      val path = new URI("file:///Users/daunnc/subversions/git/github/pomadchin/geotrellis-chatta-demo/service/geotrellis/data/arg_wm/mask.tiff")
      //  List[((KeyIndex[SpatialKey], Int, IndexRDD)])
      val (keyIndex, list) =
        GeoTiffLayerMetadata.indexSingleband(path, ZoomedLayoutScheme(WebMercator), keyIndexMethod/*, Some(0)*/)

      println(s"list.length: ${list.length}")
      //list.foreach { case (z, _) => println(z) }

      list.map { case (z, rdd) =>

        //println(ki.keyBounds)
        println(z)
        println(rdd.map { case (l, i, (ur, ar)) =>
            l -> ar.length
        }.collect.toList)
      }
    }

  }
}
