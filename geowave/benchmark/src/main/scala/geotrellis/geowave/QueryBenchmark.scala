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

package geotrellis.geowave

import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.index.SpatialTemporalElevationIndexBuilder
import geotrellis.geowave.index.dimension.ElevationDefinition
import geotrellis.geowave.index.query.ExplicitSpatialTemporalElevationQuery
import geotrellis.geowave.adapter.TypeName
import geotrellis.geowave.api._
import geotrellis.vector._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.MultibandTile
import geotrellis.store.util.BlockingThreadPool

import org.locationtech.geowave.core.geotime.index.api.{SpatialIndexBuilder, SpatialTemporalIndexBuilder}
import org.locationtech.geowave.core.store.api.{DataStore, DataStoreFactory, Index}
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}
import org.locationtech.geowave.core.geotime.store.query.{ExplicitSpatialQuery, ExplicitSpatialTemporalQuery}
import org.locationtech.geowave.core.store.CloseableIterator
import org.locationtech.geowave.datastore.cassandra.util.SessionPool
import org.locationtech.geowave.core.geotime.index.dimension.{SimpleTimeDefinition, TimeDefinition}
import org.locationtech.geowave.core.index.sfc.data.NumericRange
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass.{ConstraintData, ConstraintSet, ConstraintsByClass}
import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters._
import java.util.concurrent.TimeUnit
import java.time.{LocalDate, ZoneOffset}
import java.util.Date

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@Timeout(time = 10, timeUnit = TimeUnit.MINUTES)
class QueryBenchmark extends BenchmarkEnvironment {
  var uri: String = _

  var spatialIndex: Index = _
  var spatialTemporalIndex: Index = _
  var spatialTemporalElevationIndex: Index = _

  var typeName: TypeName = _
  var geowaveDataStore: DataStore = _

  var entireGeometry: Geometry = _

  @Setup(Level.Trial)
  def setupData(): Unit = {
    spatialIndex              = new SpatialIndexBuilder().createIndex
    spatialTemporalIndex      = new SpatialTemporalIndexBuilder().createIndex
    spatialTemporalElevationIndex = new SpatialTemporalElevationIndexBuilder().createIndex

    typeName = "BenchType".typeName
    geowaveDataStore = DataStoreFactory.createDataStore(
      new CassandraRequiredOptions(cassandra, "BenchKeyspace", new CassandraOptions()))
  }

  @TearDown(Level.Trial)
  def tearDown(): Unit = {
    val session = SessionPool.getInstance().getSession(cassandra)
    val cluster = session.getCluster
    session.close()
    cluster.close()
    BlockingThreadPool.pool.shutdown()
  }

  private def indexQuery(indexName: String) =
    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)

  private def spatialQuery(indexName: String) = {
    val sq = new ExplicitSpatialQuery(entireGeometry)
    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)
      .constraints(sq)
  }

  private def spatialTemporalQuery(indexName: String) = {
    val date = Date.from(LocalDate.of(2000, 1, 1).atStartOfDay.toInstant(ZoneOffset.UTC))
    val sq = new ExplicitSpatialTemporalQuery(date, date, entireGeometry)
    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)
      .constraints(sq)
  }

  private def spatialTemporalElevationQuery(indexName: String) = {
    val minDate = Date.from(LocalDate.ofYearDay(1970, 1).atStartOfDay(ZoneOffset.UTC).toInstant)
    val maxDate = Date.from(LocalDate.ofYearDay(2010, 1).atStartOfDay(ZoneOffset.UTC).toInstant)
    val minElevation = 0d
    val maxElevation = 25000d
    val sq = ExplicitSpatialTemporalElevationQuery(minElevation, maxElevation, minDate, maxDate, entireGeometry)

    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)
      .constraints(sq)
  }

  private def temporalQuery(indexName: String) = {
    val timeRange = new NumericRange(
      LocalDate.ofYearDay(1970, 1).atStartOfDay(ZoneOffset.UTC).toInstant.toEpochMilli.toDouble,
      LocalDate.ofYearDay(2010, 1).atStartOfDay(ZoneOffset.UTC).toInstant.toEpochMilli.toDouble
    )
    val tc = new BasicQueryByClass(new ConstraintsByClass(
      new ConstraintSet(
        new ConstraintData(timeRange, false),
        classOf[TimeDefinition],
        classOf[SimpleTimeDefinition]
      )
    ))

    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)
      .constraints(tc)
  }

  private def elevationQuery(indexName: String) = {
    val dc = new BasicQueryByClass(new ConstraintsByClass(
      new ConstraintSet(
        new ConstraintData(new NumericRange(0d, 25000d), false),
        classOf[ElevationDefinition]
      )
    ))

    SQueryBuilder
      .newBuilder[GeoTiff[MultibandTile]]
      .addTypeName(typeName.value)
      .indexName(indexName)
      .constraints(dc)
  }

  @Benchmark
  def entireSpatialQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(indexQuery(spatialIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(indexQuery(spatialTemporalIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(indexQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialGeometryQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialQuery(spatialIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalGeometryQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialQuery(spatialTemporalIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationGeometryQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalGeometryTemporalQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialTemporalQuery(spatialTemporalIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationGeometryTemporalQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialTemporalQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationGeometryTemporalElevationQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(spatialTemporalElevationQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalTemporalQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(temporalQuery(spatialTemporalIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationTemporalQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(temporalQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }

  @Benchmark
  def entireSpatialTemporalElevationElevationQuery(): List[GeoTiff[MultibandTile]] = {
    val iter: CloseableIterator[GeoTiff[MultibandTile]] =
      geowaveDataStore.query(elevationQuery(spatialTemporalElevationIndex.getName).build)
    iter.asScala.toList
  }
}
