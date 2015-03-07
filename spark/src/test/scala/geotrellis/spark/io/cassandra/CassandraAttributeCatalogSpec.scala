package geotrellis.spark.io.cassandra

import java.io.IOException

import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.raster.histogram._

import geotrellis.spark._

import geotrellis.spark.testfiles._
import geotrellis.spark.op.stats._

import spray.json._
import spray.json.DefaultJsonProtocol._
import org.joda.time.DateTime
import org.scalatest._

import org.apache.spark.SparkConf
import com.datastax.spark.connector.cql.CassandraConnector

class CassandraAttributeCatalogSpec extends FunSpec
  with Matchers
  with TestFiles
  with TestEnvironment
  with OnlyIfCanRunSpark {

  describe("Cassandra Attribute Catalog") {
    ifCanRunSpark {
      
      val conf = new SparkConf(true)
        .set("spark.cassandra.connection.host", "127.0.0.1")

      val connector = CassandraConnector(conf)
      
      val catalog: Option[CassandraAttributeCatalog] = try {
        Some(new CassandraAttributeCatalog(connector, "test", "attributes"))
      } catch {
        case _: Throwable => None
      }
      
      if (catalog.isEmpty) {
        info("No Cassandra db at 127.0.0.1; skipping tests.")
      } else {
        val attribCatalog = catalog.get
        val layerId = LayerId("test", 3)
        
        it("should save and pull out a histogram") {
          val histo = DecreasingTestFile.histogram
          
          attribCatalog.save(layerId, "histogram", histo)
          
          val loaded = attribCatalog.load[Histogram](layerId, "histogram")
          loaded.getMean should be (histo.getMean)
        }

        it("should save and load a random RootJsonReadable object") {
          case class Foo(x: Int, y: String)
          
          implicit object FooFormat extends RootJsonFormat[Foo] {
            def write(obj: Foo): JsObject =
              JsObject(
                "the_first_field" -> JsNumber(obj.x),
                "the_second_field" -> JsString(obj.y)
              )
            
            def read(json: JsValue): Foo =
              json.asJsObject.getFields("the_first_field", "the_second_field") match {
                case Seq(JsNumber(x), JsString(y)) =>
                  Foo(x.toInt, y)
                case _ =>
                  throw new DeserializationException(s"JSON reading failed.")
              }
          }
          
          val foo = Foo(1, "thing")
          
          attribCatalog.save(layerId, "foo", foo)
          attribCatalog.load[Foo](layerId, "foo") should be (foo)
        }
      }
    }
  }
}
