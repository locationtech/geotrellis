/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.store.cog

import geotrellis.tiling._
import geotrellis.raster.io._
import geotrellis.raster.histogram._
import geotrellis.layers._
import geotrellis.layers.cog.COGLayerStorageMetadata
import geotrellis.layers.index._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.testkit.testfiles.cog._
import geotrellis.spark.summary._
import geotrellis.spark.testkit._

import org.scalatest._

import spray.json._
import spray.json.DefaultJsonProtocol._

abstract class COGAttributeStoreSpec
    extends FunSpec
    with Matchers
    with TestEnvironment
    with COGTestFiles {
  def attributeStore: AttributeStore
  def header: LayerHeader

  val cogLayer = COGLayer.fromLayerRDD(spatialCea, zoomLevelCea)
  val keyIndexes = cogLayer.metadata.zoomRangeInfos.map { case (z, b) => z -> ZCurveKeyIndexMethod.createIndex(b) }.toMap
  val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)

  val layerId = LayerId("test-cog-layer", 0)

  it("should write the COGLayerAttributes") {
    attributeStore.writeCOGLayerAttributes(layerId, header, storageMetadata)
  }

  it("should read the COGLayerAttributes") {
    attributeStore.readCOGLayerAttributes[LayerHeader, COGLayerStorageMetadata[SpatialKey]](layerId)
  }

  it("should be a COGLayer") {
    attributeStore.isCOGLayer(layerId) should be (true)
  }

  it("should read the metadata of the catalog") {
    attributeStore.readMetadata[COGLayerStorageMetadata[SpatialKey]](layerId)
  }

  it("should read the header of the catalog") {
    attributeStore.readHeader[LayerHeader](layerId)
  }

  it("should read the keyIndexes of the catalog") {
    attributeStore.readKeyIndexes[SpatialKey](layerId)
  }
}
