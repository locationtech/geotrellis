package geotrellis

import org.apache.spark.rdd._

package object osm {
  type TagMap = Map[String, String]

  implicit class withElementToFeatureRDDMethods(rdd: RDD[Element]) extends ElementToFeatureRDDMethods(rdd)
}
