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

package geotrellis.process

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.testkit._
import geotrellis.raster.IntConstant

import org.scalatest._

import scala.collection.JavaConversions._
import java.io.File

class CatalogSpec extends FunSpec 
                     with Matchers 
                     with TestServer {

  val absoluteDatapath = new File("core-test/data/data").getAbsolutePath
  val datapath = "core-test/data/data"
  val datapath2 = "core-test/data/data2"

  val json0 = """
            {
              "catalog": "catalog1",
              "stores": []
            }
              """

  val json1 = s"""
              {
                "catalog": "catalog2",
                "stores": [
                  {
                    "store": "test:fs",
                    "params": {
                        "type": "fs",
                        "path": "${datapath}"
                    }
                  }
                ]
              }
            """

  val invalidJson1 = s"""
             {
               "stores" : [
               { "store": "no!", "params": { "type": "fs", "path" : "${datapath}" } }
               ]
             }
            """  

  val invalidJson2 = s"""
             {
               "catalog",
               "stores" : [
               { "store": "no!", "params": { "type": "idunno" } }
               ]
             }
            """  

  describe("A Catalog") {
    it("should load when empty") {
      val found = Catalog.fromJSON(json0)
      val expected = Catalog("catalog1", Map.empty[String, DataStore], json0, "")
      found should be (expected)
    }

    it("should load from valid JSON") {
      val jsonLoader = s"""
                        {
                          "catalog": "catalog2",
                          "stores": [
                            {
                              "store": "test:fs",
                              "params": {
                                  "type": "fs",
                                  "path": "${absoluteDatapath}"
                              }
                            }
                          ]
                        }
        """
      // note the path should be relative to catalog.json location, not from the root project
      // folder so we should not use the entire datapath

      val found = Catalog.fromJSON(jsonLoader)
      val expected = Catalog(
        "catalog2",
        Map(
          "test:fs" -> DataStore(
              "test:fs",
            new File("core-test/data/data").getAbsolutePath,
            false
          )
        ),
        jsonLoader, ""
      )
      found should be (expected)
    }

    it("should require a catalog name") {
      an [Exception] should be thrownBy {
        Catalog.fromJSON(invalidJson1)
      }
    }

    it("should require a data store path") {
      an [Exception] should be thrownBy {
        Catalog.fromJSON(invalidJson2)
      }
    }
  }

  describe("A DataStore") {
    val catalog = Catalog.fromJSON(json1)
    val store = catalog.stores("test:fs")
    val layers = store.getLayers

    it("should create IntConstant arg") {
      val result = get(io.LoadRaster("constant"))
      assert(result.asInstanceOf[ArrayRaster].data.isInstanceOf[IntConstant])
    }

    it("should correctly recognize to cache all") {
      def cacheAllLine(s:String) = s"""
{
 "catalog": "Test",
 "stores": [
  {
   "store": "test:fs",
   "params": {
     "type": "fs",
     "path": "$datapath",
     "cacheAll": "$s"
    }
  }
 ]
}
      """
      val yeses = Seq("yes","YES","True","true","1")
      val noes = Seq("no","false","shutup!","Troo")
      
      for(s <- yeses) {
        Catalog.fromJSON(cacheAllLine(s)).stores("test:fs").hasCacheAll should be (true)
      }

      for(s <- noes) {
        Catalog.fromJSON(cacheAllLine(s)).stores("test:fs").hasCacheAll should be (false)
      }
    }

    it("should not cache all when there is no cacheAll line") {
      val noCacheAllLine = s"""
{
 "catalog": "Test",
 "stores": [
  {
   "store": "test:fs",
   "params": {
     "type": "fs",
     "path": "$datapath"
    }
  }
 ]
}
      """

      val catalog = Catalog.fromJSON(noCacheAllLine)
      catalog.stores("test:fs").hasCacheAll should be (false)
    }

    it("should fail if getting by single name and same name existing two different data stores") {
      val noCacheAllLine = s"""
{
 "catalog": "Test",
 "stores": [
  {
   "store": "store1",
   "params": {
     "type": "fs",
     "path": "$datapath"
    }
  },
  {
   "store": "store2",
   "params": {
     "type": "fs",
     "path": "$datapath2"
    }
  }
 ]
}
      """

      val catalog = Catalog.fromJSON(noCacheAllLine)
      catalog.getRasterLayer(LayerId("elevation")) match {
        case scala.util.Success(_) => assert(false)
        case scala.util.Failure(_) => 
      }
    }
  
    it("should get a layer by data store and name") {
      val noCacheAllLine = s"""
{
 "catalog": "Test",
 "stores": [
  {
   "store": "store1",
   "params": {
     "type": "fs",
     "path": "$datapath"
    }
  },
  {
   "store": "store2",
   "params": {
     "type": "fs",
     "path": "$datapath2"
    }
  }
 ]
}
      """

      val catalog = Catalog.fromJSON(noCacheAllLine)

      val layer1 = catalog.getRasterLayer(LayerId("store1","quadborder")).get
      val layer2 = catalog.getRasterLayer(LayerId("store2","quadborder")).get
      layer1.info.rasterExtent should not be (layer2.info.rasterExtent)
      catalog.layerExists(LayerId("store2", "elevation")) should be (true)
      catalog.layerExists(LayerId("store2", "nonexisting-layer")) should be (false)
      catalog.layerExists(LayerId("elevation")) should be (true)
      catalog.layerExists(LayerId("nonexisting-store")) should be (false)
    }
  }
}
