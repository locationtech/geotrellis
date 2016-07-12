package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import spray.json._
import spray.json.DefaultJsonProtocol._

import org.scalatest.FunSpec

class Test extends FunSpec {
  val instance = HBaseInstance(Seq("localhost"), "localhost")
  val attributeStore = new HBaseAttributeStore(instance.getAdmin, "table")
  //attributeStore.write(LayerId("layerId", 13), "test", "string3")

  println(attributeStore.layerIds)

  println(attributeStore.layerExists(LayerId("layerId", 12)))

  println(attributeStore.readAll[String]("test"))
}
