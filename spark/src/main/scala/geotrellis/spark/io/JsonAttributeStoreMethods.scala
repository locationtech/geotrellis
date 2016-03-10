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
 *
 * WARNING:
 * The fail-over feature is a stop-gap until 0.10 release at which point it will be removed.
 * At that point this class can be used to read all layer attributes for a catalog and write them back,
 * performing an effective migration.
 */
class JsonAttributeStoreMethods(attributeStore: AttributeStore[JsonFormat]) {

  def readLayerAttributes[Header: JsonFormat, Metadata: JsonFormat, KeyIndex: JsonFormat, Schema: JsonFormat]
    (id: LayerId):(Header, Metadata, KeyIndex, Schema) = {
    val header = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metadata)
        .convertTo[Header](fieldLens(Fields.header))
    }.getOrElse {
      // Back in my day we stored header fields directly in the body
      attributeStore.cacheRead[JsObject](id, Fields.metadata)
        .convertTo[Header]
    }

    val metadata = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metadata)
        .convertTo[Metadata](fieldLens(Fields.metadata))
    }.getOrElse {
      // Back in my day we used rasterMetadata tag because it was the only possible type of metadata
      attributeStore.cacheRead[JsObject](id, Fields.metadata)
        .convertTo[Metadata](fieldLens("rasterMetadata"))
    }

    val keyIndex = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metadata)
        .convertTo[KeyIndex](fieldLens(Fields.keyIndex))
    }.getOrElse {
      // Back in my day we stored keyIndex in it's own attribute
      attributeStore.cacheRead[KeyIndex](id, Fields.keyIndex)
    }

    val schema = Try {
      attributeStore.cacheRead[JsObject](id, Fields.metadata)(fieldLens(Fields.schema))
        .convertTo[Schema](fieldLens(Fields.schema))
    }.getOrElse {
      // Back in my day we stored schema in it's own attribute
      attributeStore.cacheRead[Schema](id, Fields.schema)
    }

    (header, metadata, keyIndex, schema)
  }

  def readLayerAttribute[T: JsonFormat](id: LayerId, attributeName: String): T =
    attributeStore.cacheRead[JsObject](id, Fields.metadata).convertTo[T](fieldLens(attributeName))

  def writeLayerAttributes[Header: JsonFormat, Metadata: JsonFormat, KeyIndex: JsonFormat, Schema: JsonFormat](
    id: LayerId, header: Header, metadata: Metadata, keyIndex: KeyIndex, schema: Schema): Unit = {

    val obj = JsObject(
      Fields.header -> header.toJson,
      Fields.metadata -> metadata.toJson,
      Fields.keyIndex -> keyIndex.toJson,
      Fields.schema -> schema.toJson
    )

    import DefaultJsonProtocol._
    attributeStore.cacheWrite[JsObject](id, Fields.metadata, obj)
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
