/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.json

import geotrellis.tiling._
import geotrellis.tiling.json.KeyFormats._
import geotrellis.layers.index._
import geotrellis.layers.index.hilbert._
import geotrellis.layers.index.rowmajor._
import geotrellis.layers.index.zcurve._

import com.typesafe.config.ConfigFactory
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.mutable
import scala.reflect._


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
    private val REG_SETTING_NAME = "geotrellis.layers.index.registrator"

    private lazy val registry: Map[ClassTag[_], List[KeyIndexFormatEntry[_, _]]] = {
      val entryRegistry = new KeyIndexRegistry

      entryRegistry register KeyIndexFormatEntry[SpatialKey, HilbertSpatialKeyIndex](HilbertSpatialKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[SpatialKey, ZSpatialKeyIndex](ZSpatialKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[SpatialKey, RowMajorSpatialKeyIndex](RowMajorSpatialKeyIndexFormat.TYPE_NAME)

      entryRegistry register KeyIndexFormatEntry[SpaceTimeKey, HilbertSpaceTimeKeyIndex](HilbertSpaceTimeKeyIndexFormat.TYPE_NAME)
      entryRegistry register KeyIndexFormatEntry[SpaceTimeKey, ZSpaceTimeKeyIndex](ZSpaceTimeKeyIndexFormat.TYPE_NAME)

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
              ZSpaceTimeKeyIndex.byMilliseconds(keyBounds.convertTo[KeyBounds[SpaceTimeKey]], temporalResolution.convertTo[Long])
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
              new ZSpatialKeyIndex(kb.convertTo[KeyBounds[SpatialKey]])
            case _ =>
              throw new DeserializationException(
                "Wrong KeyIndex constructor arguments: ZSpatialKeyIndex constructor arguments expected.")
          }
        }
        case _ =>
          throw new DeserializationException("Wrong KeyIndex type: ZSpatialKeyIndex expected.")
      }
  }
}
