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

package geotrellis.doc.examples.spark

import geotrellis.layer._
import geotrellis.store._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.store._
import geotrellis.store.json._
import geotrellis.store.index._
import geotrellis.store.index.zcurve._
import geotrellis.util._

import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.semiauto._
import cats.syntax.either._

// --- //

/** A three-dimensional spatial key. A ''voxel'' is the 3D equivalent of a pixel. */
case class VoxelKey(x: Int, y: Int, z: Int)

/** Typeclass instances. These (particularly [[Boundable]]) are necessary
  * for when a layer's key type is parameterized as ''K''.
  */
object VoxelKey {
  implicit def ordering[A <: VoxelKey]: Ordering[A] =
    Ordering.by(k => (k.x, k.y, k.z))

  implicit object Boundable extends Boundable[VoxelKey] {
    def minBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.min(a.x, b.x), math.min(a.y, b.y), math.min(a.z, b.z))
    }

    def maxBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.max(a.x, b.x), math.max(a.y, b.y), math.max(a.z, b.z))
    }
  }

  /** Since [[VoxelKey]] has x and y coordinates, it can take advantage of
    * the [[SpatialComponent]] lens. Lenses are essentially "getters and setters"
    * that can be used in highly generic code.
    */
  implicit val spatialComponent = {
    Component[VoxelKey, SpatialKey](
      /* "get" a SpatialKey from VoxelKey */
      k => SpatialKey(k.x, k.y),
      /* "set" (x,y) spatial elements of a VoxelKey */
      (k, sk) => VoxelKey(sk.col, sk.row, k.z)
    )
  }

  implicit val voxelKeyEncoder: Encoder[VoxelKey] = deriveEncoder[VoxelKey]
  implicit val voxelKeyDecoder: Decoder[VoxelKey] = deriveDecoder[VoxelKey]
}

/** A [[KeyIndex]] based on [[VoxelKey]]. */
class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
  /* ''Z3'' here is a convenient shorthand for any 3-dimensional key. */
  private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

  def toIndex(k: VoxelKey): BigInt = toZ(k).z

  def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(BigInt, BigInt)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}


object ZVoxelKeyIndex {
  val voxel = "voxel"

  /** An [[Encoder]] for [[ZVoxelKeyIndex]]. */
  implicit val zVoxelKeyIndexEncoder: Encoder[ZVoxelKeyIndex] =
    Encoder.encodeJson.contramap[ZVoxelKeyIndex] { index =>
      Json.obj(
        "type" -> voxel.asJson,
        "properties" -> Json.obj("keyBounds" -> index.keyBounds.asJson)
      )
    }

  /** A [[Decoder]] for [[ZVoxelKeyIndex]]. */
  implicit val zVoxelKeyIndexDecoder: Decoder[ZVoxelKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != voxel) Left(s"Wrong KeyIndex type: $voxel expected.")
          else
            properties
              .downField("keyBounds")
              .as[KeyBounds[VoxelKey]]
              .map(new ZVoxelKeyIndex(_))
              .leftMap(_ => "Couldn't deserialize voxel key index.")

        case _ => Left("Wrong KeyIndex type: voxel key index expected.")
      }
    }

}

/** Register these Json codecs with Geotrellis's central registrator.
  * For more information on why this is necessary, see ''ShardingKeyIndex.scala''.
  */
class ZVoxelKeyIndexRegistrator extends KeyIndexRegistrator {
  def register(r: KeyIndexRegistry): Unit = {
    r.register(
      KeyIndexFormatEntry[VoxelKey, ZVoxelKeyIndex](ZVoxelKeyIndex.voxel)
    )
  }
}
