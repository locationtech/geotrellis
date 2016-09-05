package geotrellis.spark.io.geomesa

import geotrellis.spark.{LayerId, TestEnvironment}
import geotrellis.vector._
import org.apache.spark.rdd.RDD
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers, Suite}

class GeoMesaLayerWriterSpec extends FunSpec with Suite with BeforeAndAfterAll with Matchers with TestEnvironment {

  describe("The .toSimpleFeature Extension Methods") {
    val attributeStore = GeoMesaAttributeStore(instanceName = "gis", zookeepers = "localhost", user = "root", password= "secret")
    val layerWriter = new GeoMesaLayerWriter(attributeStore, "features")

    it("check rdd feature writer") {
      val features = (1 to 100)
        .map { x: Int => Feature(Point(x, 40), Map[String, Any]()) }
        .toArray
      val featureRDD: RDD[Feature[Point, Map[String, Any]]] = sc.parallelize(features)


      layerWriter.write(LayerId("gwfeature", 0), featureRDD)

      println("check")
    }
  }
}
