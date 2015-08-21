package geotrellis.spark.io.cassandra

import geotrellis.raster.histogram._
import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.op.stats._
import geotrellis.spark.testfiles._
import org.scalatest._
import spray.json._

class CassandraAttributeStoreSpec extends FunSpec
    with Matchers
    with TestFiles
    with TestEnvironment
    with OnlyIfCanRunSpark
    with SharedEmbeddedCassandra {

  describe("Cassandra Attribute Catalog") {
    ifCanRunSpark {

      useCassandraConfig(Seq("another-cassandra.yaml"))
      val host = "127.0.0.1"
      EmbeddedCassandra.withSession(host, EmbeddedCassandra.GtCassandraTestKeyspace) { implicit session =>
        val attribStore = new CassandraAttributeStore("attributes")
        val layerId = LayerId("test", 3)
        
        it("should save and pull out a histogram") {
          val histo = DecreasingTestFile.histogram
          
          attribStore.write(layerId, "histogram", histo)
          
          val loaded = attribStore.read[Histogram](layerId, "histogram")
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
          
          attribStore.write(layerId, "foo", foo)
          attribStore.read[Foo](layerId, "foo") should be (foo)
        }
      }
    }
  }
}
