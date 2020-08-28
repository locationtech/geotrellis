package geotrellis.geowave.adapter

import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.adapter.geotiff._

import geotrellis.raster.MultibandTile
import geotrellis.raster.io.geotiff.GeoTiff
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class GeoTrellisDataAdapterSPISpec extends AnyFunSpec with Matchers {
  describe("GeoTrellisDataAdapterSPISpec") {
    it("should create a GeoTiffAdapter") {
      val typeName = "TestGeoTiff".typeName

      val actual = GeoTrellisDataAdapter.load(DataTypeGeoTiff, typeName).asInstanceOf[GeoTiffAdapter]
      val expected = new GeoTiffAdapter(typeName)

      actual.getClass shouldBe expected.getClass
      actual.getTypeName shouldBe expected.getTypeName
      actual.getFieldHandlers.map { _.getFieldName } shouldBe expected.getFieldHandlers.map { _.getFieldName }
      actual.toBinary should contain theSameElementsAs expected.toBinary
    }

    it("should not create Adapters for non implemented types") {
      intercept[RuntimeException] { GeoTrellisDataAdapter.load("NewDataType".dataType, "NewTypeName".typeName) }
    }
  }
}
