package geotrellis.process

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import geotrellis.{Extent,RasterExtent}

class CatalogSpec extends Spec with MustMatchers {


val json0 = """
{
 "catalog": "catalog1",
 "stores": []
}
"""

  val json1 = """
{
 "catalog": "catalog2",
 "stores": [
  {
   "store": "stroud:fs",
   "params": {
     "type": "fs",
     "path": "src/test/resources/data"
   }
  }
 ]
}
"""

  describe("A Catalog") {
    it("should load when empty") {
      val found = Catalog.fromJSON(json0)
      val expected = Catalog("catalog1", Map.empty[String, DataStore])
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
        )
      )
      found must be === expected
    }
  }

  describe("A DataSource") {
    it("should find Arg32s in a source directory") {
      val catalog = Catalog.fromJSON(json1)
      val store = catalog.stores("stroud:fs")
      val layers = store.getLayers
      layers.toList.length must be === 3
    } 
  }

}
