package geotrellis.spark.io

import geotrellis.geomesa.geotools._
import geotrellis.util.annotations.experimental

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
package object geomesa extends GeoMesaImplicits {

  /** $experimental */
  @experimental implicit def mapToSeq[K, V](map: Map[K, V]): Seq[(K, V)] = map.toSeq
}
