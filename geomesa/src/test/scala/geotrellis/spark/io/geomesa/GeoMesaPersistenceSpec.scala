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

package geotrellis.spark.store.geomesa

import geotrellis.geomesa.geotools.{GeoMesaSimpleFeatureType, GeometryToGeoMesaSimpleFeature}
import geotrellis.vector._
import geotrellis.spark.testkit.TestEnvironment
import org.opengis.filter.Filter
import org.apache.spark.rdd.RDD
import org.geotools.data.Query
import org.geotools.filter.text.ecql.ECQL
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers, Suite}
import java.text.SimpleDateFormat
import java.util.TimeZone

import geotrellis.layers.LayerId

class GeoMesaPersistenceSpec extends FunSpec with Suite with BeforeAndAfterAll with Matchers with TestEnvironment {

  describe("GeoMesa Features Spec") {
    val featuresInstance = GeoMesaInstance(
      tableName    = "features",
      instanceName = "fake",
      zookeepers   = "localhost",
      user         = "root",
      password     = "",
      mock         = true
    )

    val featuresTemporalInstance = GeoMesaInstance(
      tableName    = "featuresTemporal",
      instanceName = "fake",
      zookeepers   = "localhost",
      user         = "root",
      password     = "",
      mock         = true
    )

    val layerWriter = new GeoMesaFeatureWriter(featuresInstance)
    val layerReader = new GeoMesaFeatureReader(featuresInstance)
    val layerWriterTemporal = new GeoMesaFeatureWriter(featuresTemporalInstance)
    val layerReaderTemporal = new GeoMesaFeatureReader(featuresTemporalInstance)

    val sdf = {
      val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      df.setTimeZone(TimeZone.getTimeZone("UTC")); df
    }

    val dates = (1 to 100).map { x =>
      val day = { val i = x / 10; if(i == 0) "01" else if (i < 10) s"0$i" else s"$i" }
      day.toInt -> sdf.parse(s"2010-05-${day}T00:00:00.000Z")
    }

    val features: Array[Feature[Point, Map[String, Any]]] = (1 to 100).map { x: Int => Feature(Point(x, 40), Map[String, Any]()) }.toArray
    val featuresTemporal: Array[Feature[Point, Map[String, Any]]] =
      (1 to 100).zip(dates).map { case (x, (day, strDay)) =>
        Feature(Point(x, 40), Map[String, Any](GeometryToGeoMesaSimpleFeature.whenField -> strDay)) }.toArray
    val featuresRDD: RDD[Feature[Point, Map[String, Any]]] = sc.parallelize(features)
    val featuresTemporalRDD: RDD[Feature[Point, Map[String, Any]]] = sc.parallelize(featuresTemporal)

    val spatialFeatureName = "spatialFeature"
    val spaceTimeFeatureName = "spaceTimeFeature"

    val spatialFeatureType = GeoMesaSimpleFeatureType[Point](spatialFeatureName)
    val spaceTimeFeatureType = GeoMesaSimpleFeatureType[Point](spaceTimeFeatureName, temporal = true)

    it("should not find layer before write") {
      val res = layerReader.read[Point, Map[String, Any]](LayerId(spatialFeatureName, 0), spatialFeatureType, new Query(spatialFeatureName, Filter.INCLUDE))
      val resTemporal = layerReaderTemporal.read[Point, Map[String, Any]](LayerId(spaceTimeFeatureName, 0), spaceTimeFeatureType, new Query(spaceTimeFeatureName, Filter.INCLUDE))
      res.count() shouldBe 0
      resTemporal.count() shouldBe 0
    }

    it("should write a layer") {
      layerWriter.write(LayerId(spatialFeatureName, 0), spatialFeatureType, featuresRDD)
      layerWriterTemporal.write(LayerId(spaceTimeFeatureName, 0), spaceTimeFeatureType, featuresTemporalRDD)
    }

    it("should read a layer back") {
      val actual =
        layerReader
          .read[Point, Map[String, Any]](LayerId(spatialFeatureName, 0), spatialFeatureType, new Query(spatialFeatureName, Filter.INCLUDE))
          .collect()

      if (features.diff(actual).nonEmpty)
        info(s"missing: ${(features diff actual).toList}")
      if (actual.diff(features).nonEmpty)
        info(s"unwanted: ${(actual diff features).toList}")

      actual should contain theSameElementsAs features
    }

    it("should read a temporal layer back") {
      val actual =
        layerReaderTemporal
          .read[Point, Map[String, Any]](LayerId(spaceTimeFeatureName, 0), spaceTimeFeatureType, new Query(spaceTimeFeatureName, Filter.INCLUDE))
          .collect()

      if (featuresTemporal.diff(actual).nonEmpty)
        info(s"missing: ${(featuresTemporal diff actual).toList}")
      if (actual.diff(featuresTemporal).nonEmpty)
        info(s"unwanted: ${(actual diff featuresTemporal).toList}")

      actual should contain theSameElementsAs featuresTemporal
    }

    it("should query a temporal layer") {
      // difference in during and between words:
      // https://github.com/locationtech/geomesa/blob/master/geomesa-filter/src/test/scala/org/locationtech/geomesa/filter/FilterHelperTest.scala#L99-L100

      val ds = dates.filter { case (k, _) => k > 3 && k < 6 }
      val expectedLength = ds.length
      val filter = ECQL.toFilter(s"${GeometryToGeoMesaSimpleFeature.whenField} between '${sdf.format(ds.head._2)}' and '${sdf.format(ds.last._2)}'")

      val actual =
        layerReaderTemporal
          .read[Point, Map[String, Any]](LayerId(spaceTimeFeatureName, 0), spaceTimeFeatureType, new Query(spaceTimeFeatureName, filter))
          .collect()

      actual.length shouldBe expectedLength
    }
  }
}
