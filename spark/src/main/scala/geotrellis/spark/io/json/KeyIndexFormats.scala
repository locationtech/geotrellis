package geotrellis.spark.io.json

import geotrellis.spark._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.hilbert._
import geotrellis.spark.io.index.rowmajor._
import geotrellis.spark.io.index.zcurve._

import com.typesafe.config.ConfigFactory
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.mutable
import scala.reflect._

import KeyFormats._

case class KeyIndexFormatEntry[K: JsonFormat: ClassTag, T <: KeyIndex[K]: JsonFormat: ClassTag](typeName: String) {
  val keyClassTag = implicitly[ClassTag[K]]
  val jsonFormat = implicitly[JsonFormat[T]]
  val indexClassTag = implicitly[ClassTag[T]]
}

trait KeyIndexRegistrator {
  def register(keyIndexRegistry: KeyIndexRegistry)
}

class KeyIndexJsonFormat[K](entries: Seq[KeyIndexFormatEntry[K, _]]) extends RootJsonFormat[KeyIndex[K]] {
  def write(obj: KeyIndex[K]): JsValue =
    entries.find { entry =>
      entry.indexClassTag.runtimeClass.getCanonicalName == obj.getClass.getCanonicalName
    } match {
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

class KeyIndexRegistry {
  private var _entries = mutable.ListBuffer[KeyIndexFormatEntry[_, _]]()
  def register(entry: KeyIndexFormatEntry[_, _]): Unit = {
    _entries += entry
  }

  def entries: List[KeyIndexFormatEntry[_, _]] =
    _entries.toList
}

trait KeyIndexFormats {
  object KeyIndexJsonFormatFactory {
    private val REG_SETTING_NAME = "geotrellis.spark.io.index.registrator"

    private lazy val registry: Map[ClassTag[_], List[KeyIndexFormatEntry[_, _]]] = {
      val entryRegistry = new KeyIndexRegistry

      entryRegistry register KeyIndexFormatEntry[GridKey, HilbertGridKeyIndex](HilbertGridKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[GridKey, ZGridKeyIndex](ZGridKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[GridKey, RowMajorGridKeyIndex](RowMajorGridKeyIndexFormat.TYPE_NAME)

      entryRegistry register KeyIndexFormatEntry[GridTimeKey, HilbertGridTimeKeyIndex](HilbertGridTimeKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[GridTimeKey, ZGridTimeKeyIndex](ZGridTimeKeyIndexFormat.TYPE_NAME)

      // User defined here
      val conf = ConfigFactory.load()
      if(conf.hasPath(REG_SETTING_NAME)) {
        val userRegistratorClassName = conf.getString(REG_SETTING_NAME)
        val userRegistrator =
          Class.forName(userRegistratorClassName)
            .getConstructor()
            .newInstance()
            .asInstanceOf[KeyIndexRegistrator]
        userRegistrator.register(entryRegistry)
      }

      entryRegistry
        .entries
        .groupBy(_.keyClassTag)
        .toMap
    }

    def getKeyIndexJsonFormat[K: ClassTag](): RootJsonFormat[KeyIndex[K]] = {
      for((key, entries) <- registry) {
        if(key == classTag[K]) {
          return new KeyIndexJsonFormat[K](entries.map(_.asInstanceOf[KeyIndexFormatEntry[K, _]]))
        }
      }
      throw new DeserializationException(s"Cannot deserialize key index for key type ${classTag[K]}. You need to register this key type using the config item $REG_SETTING_NAME")
    }
  }

  implicit def keyIndexJsonFormat[K: ClassTag]: RootJsonFormat[KeyIndex[K]] =
    KeyIndexJsonFormatFactory.getKeyIndexJsonFormat[K]

  implicit object HilbertGridKeyIndexFormat extends RootJsonFormat[HilbertGridKeyIndex] {
    final def TYPE_NAME = "hilbert"

    def write(obj: HilbertGridKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"   -> obj.keyBounds.toJson,
          "xResolution" -> obj.xResolution.toJson,
          "yResolution" -> obj.yResolution.toJson
        )
      )

    def read(value: JsValue): HilbertGridKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")
          properties.convertTo[JsObject]
            .getFields("keyBounds", "xResolution", "yResolution") match {
            case Seq(kb, xr, yr) =>
              HilbertGridKeyIndex(
                kb.convertTo[KeyBounds[GridKey]],
                xr.convertTo[Int],
                yr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: HilbertGridKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: HilbertGridKeyIndex expected.")
      }
  }

  implicit object HilbertGridTimeKeyIndexFormat extends RootJsonFormat[HilbertGridTimeKeyIndex] {
    final def TYPE_NAME = "hilbert"

    def write(obj: HilbertGridTimeKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "xResolution"        -> obj.xResolution.toJson,
          "yResolution"        -> obj.yResolution.toJson,
          "temporalResolution" -> obj.temporalResolution.toJson
        )
      )

    def read(value: JsValue): HilbertGridTimeKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject]
            .getFields("keyBounds", "xResolution", "yResolution", "temporalResolution") match {
            case Seq(kb, xr, yr, tr) =>
              HilbertGridTimeKeyIndex(
                kb.convertTo[KeyBounds[GridTimeKey]],
                xr.convertTo[Int],
                yr.convertTo[Int],
                tr.convertTo[Int]
              )
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: HilbertGridTimeKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: HilberGridTimeKeyIndex expected.")
      }
  }

  implicit object RowMajorGridKeyIndexFormat extends RootJsonFormat[RowMajorGridKeyIndex] {
    final def TYPE_NAME = "rowmajor"

    def write(obj: RowMajorGridKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): RowMajorGridKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new RowMajorGridKeyIndex(kb.convertTo[KeyBounds[GridKey]])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: RowMajorGridKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: RowMajorGridKeyIndex expected.")
      }
  }

  implicit object ZGridTimeKeyIndexFormat extends RootJsonFormat[ZGridTimeKeyIndex] {
    final def TYPE_NAME = "zorder"

    def write(obj: ZGridTimeKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject(
          "keyBounds"          -> obj.keyBounds.toJson,
          "temporalResolution" -> obj.temporalResolution.toJson
        )
      )

    def read(value: JsValue): ZGridTimeKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds", "temporalResolution") match {
            case Seq(keyBounds, temporalResolution) =>
              ZGridTimeKeyIndex.byMilliseconds(keyBounds.convertTo[KeyBounds[GridTimeKey]], temporalResolution.convertTo[Long])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: ZGridTimeKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: ZGridTimeKeyIndex expected.")
      }
  }

  implicit object ZGridKeyIndexFormat extends RootJsonFormat[ZGridKeyIndex] {
    final def TYPE_NAME = "zorder"

    def write(obj: ZGridKeyIndex): JsValue =
      JsObject(
        "type"   -> JsString(TYPE_NAME),
        "properties" -> JsObject("keyBounds" -> obj.keyBounds.toJson)
      )

    def read(value: JsValue): ZGridKeyIndex =
      value.asJsObject.getFields("type", "properties") match {
        case Seq(JsString(typeName), properties) => {
          if (typeName != TYPE_NAME)
            throw new DeserializationException(s"Wrong KeyIndex type: ${TYPE_NAME} expected.")

          properties.convertTo[JsObject].getFields("keyBounds") match {
            case Seq(kb) =>
              new ZGridKeyIndex(kb.convertTo[KeyBounds[GridKey]])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: ZGridKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: ZGridKeyIndex expected.")
      }
  }
}
