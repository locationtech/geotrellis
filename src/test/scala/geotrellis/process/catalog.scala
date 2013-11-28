package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers._
import geotrellis.{Extent,RasterExtent}
import geotrellis.raster.IntConstant
import geotrellis._
import geotrellis.testutil._

import scala.collection.JavaConversions._

class CatalogSpec extends FunSpec 
                     with MustMatchers 
                     with ShouldMatchers 
                     with TestServer {

  val datapath = "src/test/resources/data"
  val datapath2 = "src/test/resources/data2"

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
      found must be === expected
    }

    it("should load from valid JSON") {
      val found = Catalog.fromJSON(json1)
      val expected = Catalog(
        "catalog2",
        Map(
          "test:fs" -> DataStore(
              "test:fs",
              Map("type" -> "fs", "path" -> "src/test/resources/data"),
              ""
          )
        ),
        json1, ""
      )
      found must be === expected
    }

    it("should require a catalog name") {
      evaluating {
        Catalog.fromJSON(invalidJson1)
      } should produce [Exception]
    }

    it("should require a data store path") {
      evaluating {
        Catalog.fromJSON(invalidJson2)
      } should produce [Exception]
    }
  }

  describe("A DataSource") {
   val catalog = Catalog.fromJSON(json1)
   val store = catalog.stores("test:fs")
   val layers = store.getLayers

    it("should find Args in a source directory") {
      layers.toList.length must be === new java.io.File(datapath)
                                                  .listFiles
                                                  .filter { x => x.getName.endsWith(".json") }
                                                  .map { x => 1 }
                                                  .sum
    } 

    it("should create IntConstant arg") {
      val result = run(io.LoadRaster("constant"))
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

    it("should throw if getting by single name and same name existsin two different data stores") {
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
      evaluating {
        catalog.getRasterLayer("elevation")
      } should produce [Exception]
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
      val layer1 = catalog.getRasterLayer("store1","quadborder").get
      val layer2 = catalog.getRasterLayer("store2","quadborder").get
      layer1.info.rasterExtent should not be (layer2.info.rasterExtent)
    }
  }
}
