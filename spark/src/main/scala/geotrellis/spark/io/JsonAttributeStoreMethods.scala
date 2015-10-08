package geotrellis.spark.io

import geotrellis.spark.LayerId
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.util.Try
import AttributeStore.Fields

/**
 * Internally we are only using AttributeStore[JsonFormat].
 * Knowing the format allows us to abstract over writing and reading layer attributes. Layer attributes are almost
 * always required to be read and written together to perform useful work on a layer.
 *
 * We'd like to be flexible on reading attributes, attempting several fail-overs to account for changes and we'd
 * like to consolidate the writing logic such that attribute stores are as consistent as possible.
 */
class JsonAttributeStoreMethods(attributeStore: AttributeStore[JsonFormat]) {

  def readLayerAttributes[Header: JsonFormat, MetaData: JsonFormat, KeyBounds: JsonFormat, KeyIndex: JsonFormat, Schema: JsonFormat]
    (id: LayerId):(Header, MetaData, KeyBounds, KeyIndex, Schema) = {
    val header = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[Header](fieldLens(Fields.header))
    }.getOrElse {
      // Back in my day we stored header fields directly in the body
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[Header]
    }

    val metadata = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[MetaData](fieldLens(Fields.metaData))
    }.getOrElse {
      // Back in my day we used rasterMetaData tag because it was the only possible type of metadata
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[MetaData](fieldLens("rasterMetaData"))
    }

    val keyBounds = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[KeyBounds](fieldLens(Fields.keyBounds))
    }.getOrElse {
      // Back in my day we stored keyBounds in it's own attribute
      attributeStore.cacheRead[KeyBounds](id, Fields.keyBounds)
    }

    val keyIndex = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metaData)
        .convertTo[KeyIndex](fieldLens(Fields.keyIndex))
    }.getOrElse {
      // Back in my day we stored keyIndex in it's own attribute
      attributeStore.cacheRead[KeyIndex](id, Fields.keyIndex)
    }

    val schema = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metaData)(fieldLens(Fields.schema))
        .convertTo[Schema](fieldLens(Fields.schema))
    }.getOrElse {
      // Back in my day we stored schema in it's own attribute
      attributeStore.cacheRead[Schema](id, Fields.schema)
    }

    (header, metadata, keyBounds, keyIndex, schema)
  }

  def writeLayerAttributes[Header: JsonFormat, MetaData: JsonFormat, KeyBounds: JsonFormat, KeyIndex: JsonFormat, Schema: JsonFormat](
    id: LayerId, header: Header, metadata: MetaData, keyBounds: KeyBounds, keyIndex: KeyIndex, schema: Schema): Unit = {

    val obj = JsObject(
      Fields.header -> header.toJson,
      Fields.metaData -> metadata.toJson,
      Fields.keyBounds -> keyBounds.toJson,
      Fields.keyIndex -> keyIndex.toJson,
      Fields.schema -> schema.toJson
    )

    import DefaultJsonProtocol._
    attributeStore.cacheWrite[JsObject](id, Fields.metaData, obj)
  }

  private def fieldLens[T: JsonFormat](fieldName: String): JsonFormat[T] = new JsonFormat[T] {
    def read(json: JsValue) = {
      json.asJsObject.getFields(fieldName) match {
        case Seq(obj) => obj.convertTo[T]
        case _ => throw new DeserializationException(s"Could not read '$fieldName' in $json")
      }
    }

    def write(obj: T) = {
      JsObject(
        fieldName -> obj.toJson
      )
    }
  }
}
