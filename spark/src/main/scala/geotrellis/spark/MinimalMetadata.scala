package geotrellis.spark

import geotrellis.spark.io._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.GetComponent
import spray.json._
import spray.json.DefaultJsonProtocol._

// --- //

/** Minimalist Layer-level metadata. */
case class MinimalMetadata[K: JsonFormat](layout: LayoutDefinition, bounds: KeyBounds[K])

object MinimalMetadata {

  /* A Lens into the key bounds */
  implicit def minimalMetaGet[K]: GetComponent[MinimalMetadata[K], Bounds[K]] =
    GetComponent(_.bounds)

  /* Json Conversion */
  implicit def minimalMetaFormat[K: JsonFormat] = jsonFormat2(MinimalMetadata[K])

}
