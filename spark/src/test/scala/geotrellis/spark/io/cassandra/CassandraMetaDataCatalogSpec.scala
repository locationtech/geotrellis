package geotrellis.spark.io.cassandra

import java.io.IOException

import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.raster.histogram._

import geotrellis.spark._

import geotrellis.spark.testfiles._
import geotrellis.spark.op.stats._
import geotrellis.spark.io.LayerMetaData

import spray.json._
import spray.json.DefaultJsonProtocol._
import org.joda.time.DateTime
import org.scalatest._

import org.apache.spark.SparkConf
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.embedded._

class CassandraMetaDataCatalogSpec extends FunSpec
    with Matchers
    with TestFiles
    with TestEnvironment
    with OnlyIfCanRunSpark
    with SharedEmbeddedCassandra {

  describe("Cassandra MetaData Catalog") {
    ifCanRunSpark {

      useCassandraConfig("cassandra-default.yaml.template")
      val connector = EmbeddedCassandraConnector(Set(cassandraHost))
      val session = connector.openSession()

      val metaDataCatalog = new CassandraMetaDataCatalog(session, "test", "catalogs")
      val layerId = LayerId("test", 3)

      it("should save and pull out a catalog") {
        val rdd = DecreasingTestFile
        val metaData = LayerMetaData(
          keyClass = "testClass",
          rasterMetaData = rdd.metaData,
          histogram = Some(rdd.histogram)
        )
        metaDataCatalog.save(layerId, "tabletest", metaData, true)

        val loaded = metaDataCatalog.load(layerId, "tabletest")
        loaded.keyClass should be (metaData.keyClass)
        loaded.rasterMetaData should be (metaData.rasterMetaData)
        loaded.histogram should be (metaData.histogram)
      }

      it("should save and pull out a catalog with no histogram from cache and db") {
        val rdd = DecreasingTestFile
        val metaData = LayerMetaData(
          keyClass = "testClass",
          rasterMetaData = rdd.metaData,
          histogram = None
        )

        metaDataCatalog.save(layerId, "tabletest", metaData, true)

        val loaded = metaDataCatalog.load(layerId, "tabletest")
        val newCatalog = new CassandraMetaDataCatalog(session, "test", "catalogs")
        val fetched = newCatalog.load(layerId, "tabletest")

        loaded.keyClass should be (metaData.keyClass)
        loaded.rasterMetaData should be (metaData.rasterMetaData)
        loaded.histogram should be (None)

        fetched.keyClass should be (metaData.keyClass)
        fetched.rasterMetaData should be (metaData.rasterMetaData)
        fetched.histogram should be (None)
      }
    }
  }
}
