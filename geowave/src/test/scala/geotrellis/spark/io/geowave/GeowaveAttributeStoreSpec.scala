package geotrellis.spark.io.geowave

import geotrellis.spark.io.AttributeStoreSpec
import org.scalatest.BeforeAndAfter

class GeowaveAttributeStoreSpec
    extends AttributeStoreSpec {

  private def clear: Unit =
    attributeStore
      .layerIds
      .foreach(attributeStore.delete(_))

  lazy val attributeStore = new GeowaveAttributeStore(
    "leader:21810",
    "instance",
    "root",
    "password",
    "TEST"
  )

  it("should clean up after itself") {
    clear
  }
}
