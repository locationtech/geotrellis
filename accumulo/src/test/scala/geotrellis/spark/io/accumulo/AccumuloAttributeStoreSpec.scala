package geotrellis.spark.io.accumulo

import java.io.IOException

import geotrellis.raster._
import geotrellis.raster.io.json._
import geotrellis.raster.histogram._

import geotrellis.spark._

import geotrellis.spark.io._
import geotrellis.spark.testfiles._
import geotrellis.spark.summary._

import spray.json._
import spray.json.DefaultJsonProtocol._
import org.joda.time.DateTime
import org.scalatest._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

class AccumuloAttributeStoreSpec extends AttributeStoreSpec {
  val accumulo = AccumuloInstance(
    instanceName = "fake",
    zookeeper = "localhost",
    user = "root",
    token = new PasswordToken("")
  )

  lazy val attributeStore = new AccumuloAttributeStore(accumulo.connector, "attributes")
}
