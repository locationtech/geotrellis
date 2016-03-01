package geotrellis.spark.io.json

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.hilbert._
import geotrellis.spark.io.index.rowmajor._
import geotrellis.spark.io.index.zcurve._

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

case class KeyIndexFormatEntry[K: JsonFormat, T <: KeyIndex[K]: JsonFormat: ClassTag](typeName: String) {
  val jsonFormat = implicitly[JsonFormat[T]]
  val classTag = implicitly[ClassTag[T]]
}

trait KeyIndexFormats {
  implicit def optionKeyBoundsFormat[K: JsonFormat]: RootJsonFormat[Option[KeyBounds[K]]] =
    new RootJsonFormat[Option[KeyBounds[K]]] {
      def write(obj: Option[KeyBounds[K]]): JsValue =
        obj match {
          case Some(kb) => kb.toJson
          case None => JsString("none")
        }

      def read(value: JsValue): Option[KeyBounds[K]] =
        value match {
          case kb: JsObject =>
            Some(kb.convertTo[KeyBounds[K]])
          case JsString("none") =>
            None
          case _ =>
            throw new DeserializationException(s"Expected KeyBounds option, instead got $value")
        }
    }

  implicit object HilbertSpatialKeyIndexFormat extends RootJsonFormat[HilbertSpatialKeyIndex] {
    final def TYPE_NAME = "hilbert"

    def write(obj: HilbertSpatialKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"   -> obj.keyBounds.toJson,
          "xResolution" -> obj.xResolution.toJson,
          "yResolution" -> obj.yResolution.toJson
        )
      )

    def read(value: JsValue): HilbertSpatialKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")
          properties.convertTo[JsObject]
            .getFields("keyBounds", "xResolution", "yResolution") match {
            case Seq(kb, xr, yr) =>
              HilbertSpatialKeyIndex(
                kb.convertTo[KeyBounds[SpatialKey]],
                xr.convertTo[Int],
                yr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: HilbertSpatialKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: HilbertSpatialKeyIndex expected.")
      }
  }

  implicit object HilbertSpaceTimeKeyIndexFormat extends RootJsonFormat[HilbertSpaceTimeKeyIndex] {
    final def TYPE_NAME = "hilbert"

    def write(obj: HilbertSpaceTimeKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "xResolution"        -> obj.xResolution.toJson,
          "yResolution"        -> obj.yResolution.toJson,
          "temporalResolution" -> obj.temporalResolution.toJson
        )
      )

    def read(value: JsValue): HilbertSpaceTimeKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject]
            .getFields("keyBounds", "xResolution", "yResolution", "temporalResolution") match {
            case Seq(kb, xr, yr, tr) =>
              HilbertSpaceTimeKeyIndex(
                kb.convertTo[KeyBounds[SpaceTimeKey]],
                xr.convertTo[Int],
                yr.convertTo[Int],
                tr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: HilbertSpaceTimeKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: HilberSpaceTimeKeyIndex expected.")
      }
  }

  implicit object RowMajorSpatialKeyIndexFormat extends RootJsonFormat[RowMajorSpatialKeyIndex] {
    final def TYPE_NAME = "rowmajor"

    def write(obj: RowMajorSpatialKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): RowMajorSpatialKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new RowMajorSpatialKeyIndex(kb.convertTo[KeyBounds[SpatialKey]])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: RowMajorSpatialKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: RowMajorSpatialKeyIndex expected.")
      }
  }

  implicit object ZSpaceTimeKeyIndexFormat extends RootJsonFormat[ZSpaceTimeKeyIndex] {
    final def TYPE_NAME = "zorder"

    def write(obj: ZSpaceTimeKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "temporalResolution" -> obj.temporalResolution.toJson
        )
      )

    def read(value: JsValue): ZSpaceTimeKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds", "temporalResolution") match {
            case Seq(keyBounds, temporalResolution) =>
              ZSpaceTimeKeyIndex.byMilliseconds(temporalResolution.convertTo[Long])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: ZSpaceTimeKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: ZSpaceTimeKeyIndex expected.")
      }
  }

  implicit object ZSpatialKeyIndexFormat extends RootJsonFormat[ZSpatialKeyIndex] {
    final def TYPE_NAME = "zorder"

    def write(obj: ZSpatialKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): ZSpatialKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new ZSpatialKeyIndex()
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: ZSpatialKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: ZSpatialKeyIndex expected.")
      }
  }

  implicit def keyIndexFormat[K: KeyIndexJsonFormat] = new RootJsonFormat[KeyIndex[K]] {
    def write(obj: KeyIndex[K]): JsValue = {obj: KeyIndex[K]}.toJson

    /** Type cast is correct until all inner keyIndex types implement BoundedKeyIndex trait */
    def read(value: JsValue): KeyIndex[K] =
      value.asJsObject.convertTo[KeyIndex[K]].asInstanceOf[KeyIndex[K]]
  }

  trait KeyIndexJsonFormat[K] extends RootJsonFormat[KeyIndex[K]] {
    protected val entries: Vector[KeyIndexFormatEntry[K, _]]

    def write(obj: KeyIndex[K]): JsValue =
      entries.find(_.classTag.runtimeClass.isAssignableFrom(obj.getClass)) match {
        case Some(entry) =>
          obj.toJson(entry.jsonFormat.asInstanceOf[JsonFormat[KeyIndex[K]]])
        case None =>
          throw new SerializationException(s"Cannot serialize this key index with this KeyIndexJsonFormat: $obj")
      }

    def read(value: JsValue): KeyIndex[K] =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) =>
          entries.find(_.typeName == typeName) match {
            case Some(entry) =>
              // Deserialize the key index based on the entry information.
              entry.jsonFormat.read(value).asInstanceOf[KeyIndex[K]]
            case None =>
              throw new DeserializationException(s"Cannot deserialize key index with type $typeName in json $value")
          }
        case _ =>
          throw new DeserializationException(s"Expected KeyIndex, got $value")
      }
  }

  object SpatialKeyIndexJsonFormat extends KeyIndexJsonFormat[SpatialKey] {
    val entries =
      Vector(
        KeyIndexFormatEntry[SpatialKey, HilbertSpatialKeyIndex](HilbertSpatialKeyIndexFormat.TYPE_NAME),
        KeyIndexFormatEntry[SpatialKey, ZSpatialKeyIndex](ZSpatialKeyIndexFormat.TYPE_NAME),
        KeyIndexFormatEntry[SpatialKey, RowMajorSpatialKeyIndex](RowMajorSpatialKeyIndexFormat.TYPE_NAME)
      )
  }

  object SpaceTimeKeyIndexJsonFormat extends KeyIndexJsonFormat[SpaceTimeKey] {
    val entries =
      Vector(
        KeyIndexFormatEntry[SpaceTimeKey, HilbertSpaceTimeKeyIndex](HilbertSpaceTimeKeyIndexFormat.TYPE_NAME),
        KeyIndexFormatEntry[SpaceTimeKey, ZSpaceTimeKeyIndex](ZSpaceTimeKeyIndexFormat.TYPE_NAME)
      )
  }

  implicit def spatialKeyIndexJsonFormat: KeyIndexJsonFormat[SpatialKey] = SpatialKeyIndexJsonFormat
  implicit def spaceTimeKeyIndexJsonFormat: KeyIndexJsonFormat[SpaceTimeKey] = SpaceTimeKeyIndexJsonFormat

}
