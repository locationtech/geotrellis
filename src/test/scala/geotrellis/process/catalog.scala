package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import geotrellis.{Extent,RasterExtent}
import geotrellis.raster.IntConstant
import geotrellis._

import scala.collection.JavaConversions._

class CatalogSpec extends FunSpec with MustMatchers {

  val datapath = "src/test/resources/data"

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
                    "store": "stroud:fs",
                    "params": {
                        "type": "fs",
                        "path": "${datapath}"
                    }
                  }
                ]
              }
            """

  describe("A Catalog") {
    it("should load when empty") {
      val found = Catalog.fromJSON(json0)
      val expected = Catalog("catalog1", Map.empty[String, DataStore], json0, "unknown")
      found must be === expected
    }

    it("should load from valid JSON") {
      val found = Catalog.fromJSON(json1)
      val expected = Catalog(
        "catalog2",
        Map(
          "stroud:fs" -> DataStore(
              "stroud:fs",
              Map("type" -> "fs", "path" -> "src/test/resources/data")
          )
        ),
        json1,
        "unknown"
      )
      found must be === expected
    }
  }

  describe("A DataSource") {
   val catalog = Catalog.fromJSON(json1)
   val store = catalog.stores("stroud:fs")
   val layers = store.getLayers

    it("should find Args in a source directory") {
      layers.toList.length must be === new java.io.File(datapath)
                                                  .listFiles
                                                  .filter { x => x.getName.endsWith(".json") }
                                                  .map { x => 1 }
                                                  .sum
    } 

    it("should create IntConstant NODATA args") {
      val s = Server("catalogtest", catalog)
      val result = s.getRasterByName("constant", None).asInstanceOf[Result[Raster]]
      assert(result.value.data.isInstanceOf[IntConstant])
    }
  }
}
