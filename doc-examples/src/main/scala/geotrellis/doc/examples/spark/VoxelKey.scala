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

import geotrellis.tiling.{KeyBounds, SpatialKey, Boundable}
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.layers.index._
import geotrellis.layers.index.zcurve._
import geotrellis.layers.json._
import geotrellis.util._

import spray.json._

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

  /** JSON Conversion */
  implicit object VoxelKeyFormat extends RootJsonFormat[VoxelKey] {
    def write(k: VoxelKey) = {
      JsObject(
        "x" -> JsNumber(k.x),
        "y" -> JsNumber(k.y),
        "z" -> JsNumber(k.z)
      )
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("x", "y", "z") match {
        case Seq(JsNumber(x), JsNumber(y), JsNumber(z)) => VoxelKey(x.toInt, y.toInt, z.toInt)
        case _ => throw new DeserializationException("VoxelKey expected.")
      }
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
}

/** A [[KeyIndex]] based on [[VoxelKey]]. */
class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
  /* ''Z3'' here is a convenient shorthand for any 3-dimensional key. */
  private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

  def toIndex(k: VoxelKey): BigInt = toZ(k).z

  def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(BigInt, BigInt)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}

/** A [[JsonFormat]] for [[ZVoxelKeyIndex]]. */
class ZVoxelKeyIndexFormat extends RootJsonFormat[ZVoxelKeyIndex] {
  val TYPE_NAME = "voxel"

  def write(index: ZVoxelKeyIndex): JsValue = {
    JsObject(
      "type" -> JsString(TYPE_NAME),
      "properties" -> JsObject("keyBounds" -> index.keyBounds.toJson)
    )
  }

  def read(value: JsValue): ZVoxelKeyIndex = {
    value.asJsObject.getFields("type", "properties") match {
      case Seq(JsString(typeName), props) if typeName == TYPE_NAME => {
        props.asJsObject.getFields("keyBounds") match {
          case Seq(kb) => new ZVoxelKeyIndex(kb.convertTo[KeyBounds[VoxelKey]])
          case _ => throw new DeserializationException("Couldn't parse KeyBounds")
        }
      }
      case _ => throw new DeserializationException("Wrong KeyIndex type: ZVoxelKeyIndex expected.")
    }
  }
}

/** Register this JsonFormat with Geotrellis's central registrator.
  * For more information on why this is necessary, see ''ShardingKeyIndex.scala''.
  */
class ZVoxelKeyIndexRegistrator extends KeyIndexRegistrator {
  implicit val voxelFormat = new ZVoxelKeyIndexFormat()

  def register(r: KeyIndexRegistry): Unit = {
    r.register(
      KeyIndexFormatEntry[VoxelKey, ZVoxelKeyIndex](voxelFormat.TYPE_NAME)
    )
  }
}
