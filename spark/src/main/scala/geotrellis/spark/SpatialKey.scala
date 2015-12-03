package geotrellis.spark

import org.apache.spark.rdd.RDD
import spray.json._

/** A SpatialKey designates the spatial positioning of a layer's tile. */
case class SpatialKey(col: Int, row: Int) extends Product2[Int, Int] {
  def _1 = col
  def _2 = row
}

object SpatialKey {
  implicit object SpatialComponent extends IdentityComponent[SpatialKey]

  implicit def tupToKey(tup: (Int, Int)): SpatialKey =
    SpatialKey(tup._1, tup._2)

  implicit def keyToTup(key: SpatialKey): (Int, Int) =
    (key.col, key.row)

  implicit def ordering[A <: SpatialKey]: Ordering[A] =
    Ordering.by(sk => (sk.col, sk.row))

  implicit val spatialKeyFormat = new RootJsonFormat[SpatialKey] {
    def write(key: SpatialKey) =
      JsObject(
        "col" -> JsNumber(key.col),
        "row" -> JsNumber(key.row)
      )

    def read(value: JsValue): SpatialKey =
      value.asJsObject.getFields("col", "row") match {
        case Seq(JsNumber(col), JsNumber(row)) =>
          SpatialKey(col.toInt, row.toInt)
        case _ =>
          throw new DeserializationException("SpatialKey expected")
      }
  }

  implicit object Boundable extends Boundable[SpatialKey] {
    def minBound(a: SpatialKey, b: SpatialKey) = {
      SpatialKey(math.min(a.col, b.col), math.min(a.row, b.row))
    }    
    def maxBound(a: SpatialKey, b: SpatialKey) = {
      SpatialKey(math.max(a.col, b.col), math.max(a.row, b.row))
    }

    def getKeyBounds(rdd: RDD[(SpatialKey, X)] forSome {type X}): KeyBounds[SpatialKey] = {
      rdd
        .map{ case (k, tile) => KeyBounds(k, k) }
        .reduce { _ combine  _ }
    }
  }
}
