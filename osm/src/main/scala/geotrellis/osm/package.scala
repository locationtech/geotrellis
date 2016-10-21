package geotrellis

import geotrellis.util._
import geotrellis.vector._

import org.apache.spark.rdd._

// --- //

package object osm {
  type TagMap = Map[String, String]
  type OSMFeature = Feature[Geometry, Tree[ElementData]]

  implicit class withElementToFeatureRDDMethods(rdd: RDD[Element]) extends ElementToFeatureRDDMethods(rdd)
}
