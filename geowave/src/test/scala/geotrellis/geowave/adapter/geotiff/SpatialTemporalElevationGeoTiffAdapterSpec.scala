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
import geotrellis.geowave.adapter.geotiff.GeoTiffAdapter.GEOTIFF_TIME_FORMATTER_DEFAULT
import geotrellis.geowave.index.SpatialTemporalElevationIndexBuilder
import geotrellis.geowave.api.SQueryBuilder
import geotrellis.geowave.index.query.ExplicitSpatialTemporalElevationQuery
import geotrellis.raster.MultibandTile
import geotrellis.raster.io.geotiff.{GeoTiff, MultibandGeoTiff}
import geotrellis.vector.Extent

import org.locationtech.geowave.core.geotime.store.query.{ExplicitSpatialQuery, ExplicitSpatialTemporalQuery}
import org.locationtech.geowave.core.store.CloseableIterator
import org.locationtech.geowave.core.store.api.{DataStore, DataStoreFactory, Writer}
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}

import java.time.ZonedDateTime
import java.util.Date

import geotrellis.geowave.TestEnvironment
import org.scalatest.matchers.should.Matchers

class SpatialTemporalElevationGeoTiffAdapterSpec extends TestEnvironment with Matchers {
  lazy val uri: String = "src/test/resources/raster/all-ones.tif"

  describe("STDGeoTiffAdapterSpec spatial temporal elevation index spec") {
    // This describes how to index the data
    val index = new SpatialTemporalElevationIndexBuilder().createIndex

    // Register our adapter in a datastore
    lazy val geowaveDataStore: DataStore = DataStoreFactory.createDataStore(new CassandraRequiredOptions(cassandra, "STDGeoTiffAdapterSpec", new CassandraOptions()))
    lazy val tiff = MultibandGeoTiff(uri)
    lazy val dataTypeAdapter = new GeoTiffAdapter("STDGeoTiffAdapterSpec".typeName)
    geowaveDataStore.addType(dataTypeAdapter, index)

    it("should write indexed GeoTiff") {
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

    it("should query store by time") {
      val extent = tiff.extent
      val dateString = "2019:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val spatialTemporalQuery = new ExplicitSpatialTemporalQuery(date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialTemporalQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualTiff = it.next()

      actualTiff.toByteArray should contain theSameElementsAs tiff.toByteArray
      it.hasNext shouldBe false
    }

    it("should query store by elevation") {
      val extent = tiff.extent
      val dateString = "2019:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val stdQuery = ExplicitSpatialTemporalElevationQuery(1, date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(stdQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe true
      val actualTiff = it.next()

      actualTiff.toByteArray should contain theSameElementsAs tiff.toByteArray
      it.hasNext shouldBe false
    }

    it("should return nothing querying out of the spatial tiff bounds") {
      val extent = Extent(0, 0, 1, 1)
      val dateString = "2019:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val spatialTemporalQuery = new ExplicitSpatialTemporalQuery(date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialTemporalQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should return nothing querying out of the temporal bounds") {
      val extent = tiff.extent
      val dateString = "2017:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val spatialTemporalQuery = new ExplicitSpatialTemporalQuery(date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialTemporalQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should query nothing out of the elevation bounds") {
      val extent = tiff.extent
      val dateString = "2019:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val stdQuery = ExplicitSpatialTemporalElevationQuery(100, date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(stdQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should return nothing querying out of the spatialtemporal bounds") {
      val extent = Extent(0, 0, 1, 1)
      val dateString = "2017:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val spatialTemporalQuery = new ExplicitSpatialTemporalQuery(date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialTemporalQuery)

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }

    it("should delete nothing by the query out of bounds") {
      val extent = Extent(0, 0, 1, 1)
      val dateString = "2017:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val spatialTemporalQuery = new ExplicitSpatialTemporalQuery(date, date, extent.toPolygon())

      val dquery =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(spatialTemporalQuery)

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
      val dateString = "2019:11:01 12:00:00"
      val date = Date.from(ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse(dateString)).toInstant)
      val stdQuery = ExplicitSpatialTemporalElevationQuery(1, date, date, extent.toPolygon())

      val query =
        SQueryBuilder
          .newBuilder[GeoTiff[MultibandTile]]
          .addTypeName(dataTypeAdapter.getTypeName)
          .indexName(index.getName)
          .constraints(stdQuery)

      geowaveDataStore.delete(query.build()) shouldBe true

      val it: CloseableIterator[GeoTiff[MultibandTile]] = geowaveDataStore.query(query.build())
      it.hasNext shouldBe false
    }
  }
}
