/*
 * Copyright 2021 Azavea
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
