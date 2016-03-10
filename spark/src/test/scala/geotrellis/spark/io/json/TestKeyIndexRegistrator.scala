package geotrellis.spark.io.json

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._

import spray.json._
import spray.json.DefaultJsonProtocol._

class TestKeyIndex(val keyBounds: KeyBounds[GridKey]) extends KeyIndex[GridKey] {
  def toIndex(key: GridKey): Long = 1L

  def indexRanges(keyRange: (GridKey, GridKey)): Seq[(Long, Long)] =
    Seq((1L, 2L))
}

class TestKeyIndexRegistrator extends KeyIndexRegistrator {
  implicit object TestKeyIndexFormat extends RootJsonFormat[TestKeyIndex] {
    final def TYPE_NAME = "test"

    def write(obj: TestKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): TestKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new TestKeyIndex(kb.convertTo[KeyBounds[GridKey]])
            case _ =>
              throw new DeserializationException(
                "Couldn't deserialize test key index.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: test key index expected.")
      }
  }

  def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
    keyIndexRegistry register KeyIndexFormatEntry[GridKey, TestKeyIndex](TestKeyIndexFormat.TYPE_NAME)
  }
}
