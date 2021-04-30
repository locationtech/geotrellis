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

package geotrellis.geowave.adapter.geotiff

import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.api.SQueryBuilder
import geotrellis.raster.MultibandTile
import geotrellis.raster.io.geotiff.{GeoTiff, MultibandGeoTiff}
import geotrellis.vector.Extent
import org.locationtech.geowave.core.geotime.index.api.SpatialIndexBuilder
import org.locationtech.geowave.core.geotime.store.query.ExplicitSpatialQuery
import org.locationtech.geowave.core.store.CloseableIterator
import org.locationtech.geowave.core.store.api.{DataStore, DataStoreFactory, Writer}
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}

import geotrellis.geowave.TestEnvironment
import org.scalatest.matchers.should.Matchers

class GeoTiffAdapterSpec extends TestEnvironment with Matchers {
  lazy val uri: String = "src/test/resources/raster/all-ones.tif"

  describe("GeoTiffAdapterSpec spatial index spec") {
    // This describes how to index the data
    val index = new SpatialIndexBuilder().createIndex

    // Register our adapter in a datastore
    lazy val geowaveDataStore: DataStore = DataStoreFactory.createDataStore(new CassandraRequiredOptions(cassandra, "GeoTiffAdapterSpec", new CassandraOptions()))
    lazy val tiff = MultibandGeoTiff(uri)
    lazy val dataTypeAdapter = new GeoTiffAdapter("GeoTiffAdapterSpec".typeName)
    geowaveDataStore.addType(dataTypeAdapter, index)

    it("should write spatially indexed GeoTiff") {
      val indexWriter: Writer[GeoTiff[MultibandTile]] = geowaveDataStore.createWriter(dataTypeAdapter.getTypeName)
      try indexWriter.write(tiff) finally {
        if (indexWriter != null) indexWriter.close()
      }
    }

    it("should query the entire contents of a store") {
      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualTiff = it.next()
      actualTiff.toByteArray should contain theSameElementsAs tiff.toByteArray
      it.hasNext shouldBe false
    }

    it("should query store by extent") {
      val extent = tiff.extent
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualTiff = it.next()

      actualTiff.toByteArray should contain theSameElementsAs tiff.toByteArray
      it.hasNext shouldBe false
    }

    it("should return nothing querying out of the tiff bounds") {
      val extent = Extent(0, 0, 1, 1)
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should delete nothing by the query out of bounds") {
      val extent = Extent(0, 0, 1, 1)
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val dquery =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      geowaveDataStore.delete(dquery.build()) shouldBe true

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
    }

    it("should delete everything by the query") {
      val extent = tiff.extent
      val spatialQuery = new ExplicitSpatialQuery(extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialQuery)

      geowaveDataStore.delete(query.build()) shouldBe true

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }
  }

  describe("GeoTiffAdapterSpec serialization spec") {
    it("should be serialized properly") {
      val expected = new GeoTiffAdapter("MultibandGeoTiffDataAdapterSpec".typeName)
      val actual   = new GeoTiffAdapter()

      actual.fromBinary(expected.toBinary)

      actual.getTypeName shouldBe expected.getTypeName
      actual.getFieldHandlers.map { _.getFieldName } shouldBe expected.getFieldHandlers.map { _.getFieldName }
    }
  }
}
