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

package geotrellis.spark.io.geowave

import geotrellis.tiling.SpatialKey
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.layers.index.ZCurveKeyIndexMethod
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.SocketWriteStrategy
import geotrellis.spark.testkit.testfiles.TestFiles
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.datastore.accumulo._
import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.GeneralParameterValue
import org.scalatest._

class GeoWaveSpatialSpec
    extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with GeoWaveTestEnvironment
{

  val gwNamespace = "TEST"

  val attributeStore = new GeoWaveAttributeStore(
    "leader:21810",
    "instance",
    "root",
    "password",
    gwNamespace
  )

  val reader = new GeoWaveLayerReader(attributeStore)
  val writer = new GeoWaveLayerWriter(attributeStore, SocketWriteStrategy())
  val coverageName = "Sample Elevation 1"
  val id1 = LayerId(coverageName, 11)
  val id2 = LayerId("Sample Elevation 2", 11)

  def getGridCoverage2D(filename: String): GridCoverage2D = {
    val file = new java.io.File(filename)
    val params = Array[GeneralParameterValue]()

    new GeoTiffReader(file).read(params)
  }

  def poke(bo: BasicAccumuloOperations, img: GridCoverage2D): Unit = {
    val metadata = new java.util.HashMap[String, String]()
    val dataStore = new AccumuloDataStore(bo)
    val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).createIndex()
    val adapter = new RasterDataAdapter(coverageName, metadata, img, 256, true) // img only used for metadata, not data
    val indexWriter = dataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[GridCoverage]]

    indexWriter.write(img)
    indexWriter.close
  }

  def clear(): Unit = {
    attributeStore.delete(s"${gwNamespace}_GEOWAVE_METADATA")
    attributeStore.delete(s"${gwNamespace}_SPATIAL_IDX")
  }

  it("should not find layer before write") {
    intercept[LayerNotFoundError] {
      reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id1)
    }
  }

  it("should read an existing layer") {
    val img = getGridCoverage2D("spark/src/test/resources/elevation.tif")
    val bo = attributeStore.basicAccumuloOperations

    poke(bo, img)

    val layer = reader
      .read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id1)
      .map({ kv => 1 })
      .collect()

    layer.length should be (6)
  }

  it("should write a layer") {
    val layer = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id1)

    writer.write(id2, layer)
  }

  it("should read a layer back") {
    val original = reader
      .read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id1)
      .keys.count
    val geowave = reader
      .read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](id2)
      .keys.count

    original should be (geowave)
  }

  it("should clean up after itself") {
    clear
  }
}
