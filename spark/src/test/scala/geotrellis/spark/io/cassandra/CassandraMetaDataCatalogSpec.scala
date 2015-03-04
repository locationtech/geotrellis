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

      val catalog: Option[CassandraMetaDataCatalog] = try {
        Some(new CassandraMetaDataCatalog(connector, "test", "catalogs"))
      } catch {
        case _: IOException => None
      }

      if (catalog.isEmpty) {
        info("No Cassandra db at 127.0.0.1; skipping tests.")
      } else {
        val metaDataCatalog = catalog.get
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
          val newCatalog = new CassandraMetaDataCatalog(connector, "test", "catalogs")
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
}
