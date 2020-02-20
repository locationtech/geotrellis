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

package geotrellis.store.json

import geotrellis.layer._
import geotrellis.store.index._
import geotrellis.store.index.hilbert._
import geotrellis.store.index.rowmajor._
import geotrellis.store.index.zcurve._

import io.circe._
import io.circe.syntax._
import cats.syntax.either._
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.reflect._


case class KeyIndexFormatEntry[K: Encoder: Decoder: ClassTag, T <: KeyIndex[K]: Encoder: Decoder: ClassTag](typeName: String) {
  val keyClassTag = implicitly[ClassTag[K]]
  val jsonEncoder = implicitly[Encoder[T]]
  val jsonDecoder = implicitly[Decoder[T]]
  val indexClassTag = implicitly[ClassTag[T]]
}

trait KeyIndexRegistrator {
  def register(keyIndexRegistry: KeyIndexRegistry)
}

class KeyIndexEncoder[K](entries: Seq[KeyIndexFormatEntry[K, _]]) extends Encoder[KeyIndex[K]] {
  final def apply(obj: KeyIndex[K]): Json =
    entries.find { entry =>
      entry.indexClassTag.runtimeClass.getCanonicalName == obj.getClass.getCanonicalName
    } match {
      case Some(entry) =>
        obj.asJson(entry.jsonEncoder.asInstanceOf[Encoder[KeyIndex[K]]])
      case None =>
        throw DecodingFailure(s"Cannot serialize this key index with this KeyIndexJsonFormat: $obj", Nil)
    }
}

class KeyIndexDecoder[K](entries: Seq[KeyIndexFormatEntry[K, _]]) extends Decoder[KeyIndex[K]] {
  final def apply(c: HCursor): Decoder.Result[KeyIndex[K]] = {
    (c.downField("type").as[String], c.downField("properties").focus) match {
      case (Right(typeName), _) =>
        entries.find(_.typeName == typeName) match {
          case Some(entry) =>
            // Deserialize the key index based on the entry information.
            entry.jsonDecoder(c).map(_.asInstanceOf[KeyIndex[K]])
          case None =>
            throw DecodingFailure(s"Cannot deserialize key index with type $typeName in json ${c.focus}", Nil)
        }
      case _ => throw DecodingFailure(s"Expected KeyIndex, got ${c.focus}", Nil)
    }
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
  val hilbert: String = "hilbert"
  val zorder: String = "zorder"
  val rowmajor: String = "rowmajor"

  object KeyIndexJsonFormatFactory {
    private val REG_SETTING_NAME = "geotrellis.store.index.registrator"

    private lazy val registry: Map[ClassTag[_], List[KeyIndexFormatEntry[_, _]]] = {
      val entryRegistry = new KeyIndexRegistry

      entryRegistry register KeyIndexFormatEntry[SpatialKey, HilbertSpatialKeyIndex](hilbert)
      entryRegistry register KeyIndexFormatEntry[SpatialKey, ZSpatialKeyIndex](zorder)
      entryRegistry register KeyIndexFormatEntry[SpatialKey, RowMajorSpatialKeyIndex](rowmajor)

      entryRegistry register KeyIndexFormatEntry[SpaceTimeKey, HilbertSpaceTimeKeyIndex](hilbert)
      entryRegistry register KeyIndexFormatEntry[SpaceTimeKey, ZSpaceTimeKeyIndex](zorder)

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

    def getKeyIndexEncoder[K: ClassTag](): Encoder[KeyIndex[K]] = {
      for((key, entries) <- registry) {
        if(key == classTag[K]) {
          return new KeyIndexEncoder[K](entries.map(_.asInstanceOf[KeyIndexFormatEntry[K, _]]))
        }
      }
      throw DecodingFailure(s"Cannot deserialize key index for key type ${classTag[K]}. You need to register this key type using the config item $REG_SETTING_NAME", Nil)
    }

    def getKeyIndexDecoder[K: ClassTag](): Decoder[KeyIndex[K]] = {
      for((key, entries) <- registry) {
        if(key == classTag[K]) {
          return new KeyIndexDecoder[K](entries.map(_.asInstanceOf[KeyIndexFormatEntry[K, _]]))
        }
      }
      throw DecodingFailure(s"Cannot deserialize key index for key type ${classTag[K]}. You need to register this key type using the config item $REG_SETTING_NAME", Nil)
    }
  }

  implicit def keyIndexEncoder[K: ClassTag]: Encoder[KeyIndex[K]] = KeyIndexJsonFormatFactory.getKeyIndexEncoder[K]
  implicit def keyIndexDecoder[K: ClassTag]: Decoder[KeyIndex[K]] = KeyIndexJsonFormatFactory.getKeyIndexDecoder[K]

  implicit val hilbertSpatialKeyIndexEncoder: Encoder[HilbertSpatialKeyIndex] =
    Encoder.encodeJson.contramap[HilbertSpatialKeyIndex] { obj =>
      Json.obj(
        "type"   -> hilbert.asJson,
        "properties" -> Json.obj(
          "keyBounds"   -> obj.keyBounds.asJson,
          "xResolution" -> obj.xResolution.asJson,
          "yResolution" -> obj.yResolution.asJson
        )
      )
    }

  implicit val hilbertSpatialKeyIndexDecoder: Decoder[HilbertSpatialKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != hilbert) Left(s"Wrong KeyIndex type: $hilbert expected.")
          else {
            (properties.downField("keyBounds").as[KeyBounds[SpatialKey]],
            properties.downField("xResolution").as[Int],
            properties.downField("yResolution").as[Int]) match {
              case (Right(kb), Right(xr), Right(yr)) => Right(HilbertSpatialKeyIndex(kb, xr, yr))
              case _ => Left("Wrong KeyIndex constructor arguments: HilbertSpatialKeyIndex constructor arguments expected.")
            }
          }

        case _ => Left("Wrong KeyIndex type: HilbertSpatialKeyIndex expected.")
      }
    }

  implicit val hilbertSpaсeTimeKeyIndexEncoder: Encoder[HilbertSpaceTimeKeyIndex] =
    Encoder.encodeJson.contramap[HilbertSpaceTimeKeyIndex] { obj =>
      Json.obj(
        "type"   -> hilbert.asJson,
        "properties" -> Json.obj(
          "keyBounds"          -> obj.keyBounds.asJson,
          "xResolution"        -> obj.xResolution.asJson,
          "yResolution"        -> obj.yResolution.asJson,
          "temporalResolution" -> obj.temporalResolution.asJson
        )
      )
    }

  implicit val hilbertSpaceTimeKeyIndexDecoder: Decoder[HilbertSpaceTimeKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != hilbert) Left(s"Wrong KeyIndex type: $hilbert expected.")
          else {
            (properties.downField("keyBounds").as[KeyBounds[SpaceTimeKey]],
              properties.downField("xResolution").as[Int],
              properties.downField("yResolution").as[Int],
              properties.downField("temporalResolution").as[Int]) match {
              case (Right(kb), Right(xr), Right(yr), Right(tr)) => Right(HilbertSpaceTimeKeyIndex(kb, xr, yr, tr))
              case _ => Left("Wrong KeyIndex constructor arguments: HilbertSpaceTimeKeyIndex constructor arguments expected.")
            }
          }

        case _ => Left("Wrong KeyIndex type: HilberSpaceTimeKeyIndex expected.")
      }
    }

  implicit val rowMajorSpatialKeyIndexEncoder: Encoder[RowMajorSpatialKeyIndex] =
    Encoder.encodeJson.contramap[RowMajorSpatialKeyIndex] { obj =>
      Json.obj(
        "type"   -> rowmajor.asJson,
        "properties" -> Json.obj(
          "keyBounds"   -> obj.keyBounds.asJson
        )
      )
    }

  implicit val rowMajorSpatialKeyIndexDecoder: Decoder[RowMajorSpatialKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != rowmajor) Left(s"Wrong KeyIndex type: $rowmajor expected.")
          else
            properties
              .downField("keyBounds")
              .as[KeyBounds[SpatialKey]]
              .map(new RowMajorSpatialKeyIndex(_))
              .leftMap(_ => "Wrong KeyIndex constructor arguments: RowMajorSpatialKeyIndex constructor arguments expected.")

        case _ => Left("Wrong KeyIndex type: RowMajorSpatialKeyIndex expected.")
      }
    }

  implicit val zSpatialKeyIndexEncoder: Encoder[ZSpatialKeyIndex] =
    Encoder.encodeJson.contramap[ZSpatialKeyIndex] { obj =>
      Json.obj(
        "type"   -> zorder.asJson,
        "properties" -> Json.obj(
          "keyBounds"   -> obj.keyBounds.asJson
        )
      )
    }

  implicit val zSpatialKeyIndexDecoder: Decoder[ZSpatialKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != zorder) Left(s"Wrong KeyIndex type: $zorder expected.")
          else properties
            .downField("keyBounds")
            .as[KeyBounds[SpatialKey]]
            .map(new ZSpatialKeyIndex(_))
            .leftMap(_ => "Wrong KeyIndex constructor arguments: ZSpatialKeyIndex constructor arguments expected.")

        case _ => Left("Wrong KeyIndex type: ZSpatialKeyIndex expected.")
      }
    }

  implicit val zSpaceTimeKeyIndexEncoder: Encoder[ZSpaceTimeKeyIndex] =
    Encoder.encodeJson.contramap[ZSpaceTimeKeyIndex] { obj =>
      Json.obj(
        "type"   -> zorder.asJson,
        "properties" -> Json.obj(
          "keyBounds"   -> obj.keyBounds.asJson,
          "temporalResolution" -> obj.temporalResolution.asJson
        )
      )
    }

  implicit val zSpaceTimeKeyIndexDecoder: Decoder[ZSpaceTimeKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != zorder) Left(s"Wrong KeyIndex type: $zorder expected.")
          else {
            (properties.downField("keyBounds").as[KeyBounds[SpaceTimeKey]],
              properties.downField("temporalResolution").as[Long]) match {
              case (Right(keyBounds), Right(tr)) => Right(ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, tr))
              case _ => Left("Wrong KeyIndex constructor arguments: ZSpaceTimeKeyIndex constructor arguments expected.")
            }
          }

        case _ => Left("Wrong KeyIndex type: ZSpaceTimeKeyIndex expected.")
      }
    }
}
