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

class CassandraMetaDataCatalogSpec extends FunSpec
                                   with Matchers
                                   with TestFiles
                                   with TestEnvironment
                                   with OnlyIfCanRunSpark {

  describe("Cassandra MetaData Catalog") {
    ifCanRunSpark {

      val conf = new SparkConf(true)
        .set("spark.cassandra.connection.host", "127.0.0.1")

      val connector = CassandraConnector(conf)

      val metaDataCatalog = new CassandraMetaDataCatalog(connector, "test", "catalogs")
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
    }
  }
}
