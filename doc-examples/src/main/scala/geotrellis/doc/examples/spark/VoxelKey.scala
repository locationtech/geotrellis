package geotrellis.doc.examples.spark

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve._
import geotrellis.spark.io.json._

import spray.json._

// --- //

/** A three-dimensional spatial key. */
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
}

/** A [[KeyIndex]] based on [[VoxelKey]]. */
class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
  private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

  def toIndex(k: VoxelKey): Long = toZ(k).z

  def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(Long, Long)] =
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

/** Register this JsonFormat with Geotrellis's central registrator. */
class ZVoxelKeyIndexRegistrator extends KeyIndexRegistrator {
  implicit val voxelFormat = new ZVoxelKeyIndexFormat()

  def register(r: KeyIndexRegistry): Unit = {
    r.register(
      KeyIndexFormatEntry[VoxelKey, ZVoxelKeyIndex](voxelFormat.TYPE_NAME)
    )
  }
}
