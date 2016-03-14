package geotrellis.spark.io.json

import geotrellis.spark._
import geotrellis.spark.io.index._

import org.scalatest._
import spray.json._

class KeyIndexJsonFormatFactorySpec extends FunSpec with Matchers {
  describe("KeyIndexJsonFormatFactory"){
    it("should be able to serialize and deserialize a custom key index set through application.conf") {
      val expectedKeyBounds = KeyBounds(SpatialKey(1, 2), SpatialKey(5, 6))
      val testKeyIndex: KeyIndex[SpatialKey] = new TestKeyIndex(expectedKeyBounds)
      val json = testKeyIndex.toJson
      val actual = json.convertTo[KeyIndex[SpatialKey]]
      actual.keyBounds should be (expectedKeyBounds)
      actual should be (a[TestKeyIndex])
    }
  }
}
