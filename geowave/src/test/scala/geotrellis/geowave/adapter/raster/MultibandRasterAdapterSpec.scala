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

package geotrellis.geowave.adapter.raster

import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.api.SQueryBuilder
import geotrellis.raster.io.geotiff.MultibandGeoTiff
import geotrellis.raster.{MultibandTile, Raster}
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.vector.Extent
import org.locationtech.geowave.core.geotime.index.api.SpatialIndexBuilder
import org.locationtech.geowave.core.geotime.store.query.ExplicitSpatialQuery
import org.locationtech.geowave.core.store.CloseableIterator
import org.locationtech.geowave.core.store.api.{DataStore, DataStoreFactory, Writer}
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}
import geotrellis.geowave.TestEnvironment
import geotrellis.geowave.api.SQueryBuilder
import org.scalatest.matchers.should.Matchers

class MultibandRasterAdapterSpec extends TestEnvironment with RasterMatchers with Matchers {
  lazy val uri: String = "src/test/resources/raster/all-ones.tif"

  describe("MultibandRasterAdapterSpec spatial index spec") {
    // This describes how to index the data
    val index = new SpatialIndexBuilder().createIndex

    // Register our adapter in a datastore
    lazy val geowaveDataStore: DataStore = DataStoreFactory.createDataStore(new CassandraRequiredOptions(cassandra, "MultibandRasterAdapterSpec", new CassandraOptions()))
    lazy val raster = MultibandGeoTiff(uri).raster
    lazy val dataTypeAdapter = new MulitbandRasterAdapter("MultibandRasterAdapterSpec".typeName)
    geowaveDataStore.addType(dataTypeAdapter, index)

    it("should write spatially indexed Raster") {
      val indexWriter: Writer[Raster[MultibandTile]] = geowaveDataStore.createWriter(dataTypeAdapter.getTypeName)
      try indexWriter.write(raster) finally {
        if (indexWriter != null) indexWriter.close()
      }
    }

    it("should query the entire contents of a store") {
      val query =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)

      val it: CloseableIterator[Raster[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualRaster = it.next()
      assertEqual(actualRaster,  raster)
      it.hasNext shouldBe false
    }

    it("should query store by extent") {
      val extent = raster.extent
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      val it: CloseableIterator[Raster[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualRaster = it.next()
      assertEqual(actualRaster, raster)
      it.hasNext shouldBe false
    }

    it("should return nothing querying out of the tiff bounds") {
      val extent = Extent(0, 0, 1, 1)
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      val it: CloseableIterator[Raster[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should delete nothing by the query out of bounds") {
      val extent = Extent(0, 0, 1, 1)
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val dquery =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      geowaveDataStore.delete(dquery.build()) shouldBe true

      val query =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)

      val it: CloseableIterator[Raster[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
    }

    it("should delete everything by the query") {
      val extent = raster.extent
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[Raster[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      geowaveDataStore.delete(query.build()) shouldBe true

      val it: CloseableIterator[Raster[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }
  }

  describe("MultibandRasterAdapterSpec serialization spec") {
    it("should be serialized properly") {
      val expected = new MulitbandRasterAdapter("MultibandRasterAdapterSpec".typeName)
      val actual   = new MulitbandRasterAdapter()

      actual.fromBinary(expected.toBinary)

      actual.getTypeName shouldBe expected.getTypeName
      actual.getFieldHandlers.map { _.getFieldName } shouldBe expected.getFieldHandlers.map { _.getFieldName }
    }
  }
}
