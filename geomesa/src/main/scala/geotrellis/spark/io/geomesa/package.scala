package geotrellis.spark.io

import geotrellis.geomesa.geotools._

package object geomesa extends GeoMesaImplicits {
  implicit def mapToSeq[K, V](map: Map[K, V]): Seq[(K, V)] = map.toSeq
}
