package geotrellis.spark.io

import geotrellis.spark.{SpaceTimeKey, LayerId, SpatialKey}
import org.scalatest._
class DefaultParamsSpec extends FunSpec with Matchers {

  describe("Default Params"){
    it("should produce default based on key"){
      val expectedTable = "spatial-table"
      val params = new DefaultParams[String]
        .withKeyParams[SpatialKey](expectedTable)

      params.paramsFor[SpatialKey](LayerId("ones", 10)) should be (Some(expectedTable))
      params.paramsFor[SpaceTimeKey](LayerId("ones", 10)) should be (None)
    }

    it("should use layer rules first"){
      val expectedTable = "spatial-table"
      val params = new DefaultParams[String]
        .withKeyParams[SpatialKey]("default-key-table")
        .withLayerParams[SpatialKey]{ case id => expectedTable }

      params.paramsFor[SpatialKey](LayerId("ones", 10)) should be (Some(expectedTable))
    }

    it("should overwrite rule for the same key"){
      val expectedTable = "spatial-table"
      val params = new DefaultParams[String]
        .withLayerParams[SpatialKey]{ case id => "unexpected-table" }
        .withLayerParams[SpatialKey]{ case id => expectedTable }

      params.paramsFor[SpatialKey](LayerId("ones", 10)) should be (Some(expectedTable))
    }
  }
}
