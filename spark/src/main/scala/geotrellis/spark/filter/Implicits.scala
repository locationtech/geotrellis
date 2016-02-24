package geotrellis.spark.filter

import geotrellis.spark.SpaceTimeKey


object Implicits extends Implicits

trait Implicits {
  implicit def SpaceTimeToSpatial(st: SpaceTimeKey) = st.spatialKey
}
