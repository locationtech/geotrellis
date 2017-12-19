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

package geotrellis.spark.io.cog

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.proj4._
import geotrellis.raster.Tile
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.testkit._
import geotrellis.spark.io.file.FileAttributeStore
import geotrellis.spark.io.file.cog._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import org.apache.hadoop.fs.Path
import org.scalatest._

import scala.collection.mutable.ListBuffer

class COGLayerSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("COGLayer") {

    it("should write GeoTrellis COGLayer") {
      val source = sc.hadoopGeoTiffRDD(new Path("file:///Users/daunnc/subversions/git/github/pomadchin/geotrellis/raster/data/geotiff-test-files/reproject/cea.tif"))
      val list: ListBuffer[(Int, TileLayerRDD[SpatialKey])] = ListBuffer()
      val layoutScheme = ZoomedLayoutScheme(LatLng, 256)

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (rdd, zoom) =>
        list += zoom -> rdd
      }

      val (zoom, layer) = list.head
      val index: ZSpatialKeyIndex = new ZSpatialKeyIndex(layer.metadata.bounds match {
        case kb: KeyBounds[SpatialKey] => kb
        case _ => null
      })

      // Create the attributes store that will tell us information about our catalog.
      val attributeStore = FileAttributeStore("/data/test-new")

      // Create the writer that we will use to store the tiles in the local catalog.
      val writer = new FileCOGLayerWriter2(attributeStore)

      /*val f: Iterable[(SpatialKey, Tile)] => GeoTiffSegmentConstructMethods[SpatialKey, Tile] =
        iter => withSinglebandGeoTiffSegmentConstructMethods(iter)*/


      writer.write("landsat_cog", layer, zoom, ZCurveKeyIndexMethod)
    }

    it("should read GeoTrellis COGLayer") {
      // Create the attributes store that will tell us information about our catalog.
      val attributeStore = FileAttributeStore("/data/test-new")

      // Create the writer that we will use to store the tiles in the local catalog.
      val reader = new FileCOGValueReader2(attributeStore, "/data/test-new")

      /*val f: Iterable[(SpatialKey, Tile)] => GeoTiffSegmentConstructMethods[SpatialKey, Tile] =
        iter => withSinglebandGeoTiffSegmentConstructMethods(iter)*/


      reader.reader[SpatialKey, Tile](LayerId("landsat_cog", 9)).read(SpatialKey(44,79))
      //writer.write("landsat_cog", layer, zoom, ZCurveKeyIndexMethod)
    }
  }

}
