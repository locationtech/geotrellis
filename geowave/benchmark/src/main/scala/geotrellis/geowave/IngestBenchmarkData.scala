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
import org.locationtech.geowave.core.geotime.index.api.{SpatialIndexBuilder, SpatialTemporalIndexBuilder}
import org.locationtech.geowave.core.store.api.{DataStoreFactory, Writer}
import org.locationtech.geowave.datastore.cassandra.config.{CassandraOptions, CassandraRequiredOptions}
import org.locationtech.geowave.datastore.cassandra.util.SessionPool
import cats.syntax.flatMap._
import cats.syntax.parallel._
import cats.instances.list._
import cats.effect.IO
import geotrellis.geowave.adapter.geotiff.GeoTiffAdapter
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.MultibandTile
import geotrellis.store.util.BlockingThreadPool

object IngestBenchmarkData extends BenchmarkEnvironment {
  def main(args: Array[String]): Unit = {
    val n: Int = args.headOption.map(_.toInt).getOrElse(20)

    val spatialIndex              = new SpatialIndexBuilder().createIndex
    val spatialTemporalIndex      = new SpatialTemporalIndexBuilder().createIndex
    val spatialTemporalDepthIndex = new SpatialTemporalElevationIndexBuilder().createIndex

    val dataTypeAdapter = new GeoTiffAdapter("QueryBench".typeName)
    val geowaveDataStore = DataStoreFactory.createDataStore(new CassandraRequiredOptions(cassandra, "QueryBench", new CassandraOptions()))
    geowaveDataStore.addType(dataTypeAdapter, spatialIndex, spatialTemporalIndex, spatialTemporalDepthIndex)

    val data: IO[List[GeoTiff[MultibandTile]]] = ???
    val result = (data >>= { tiles =>
      tiles.map { tile => IO {
        val indexWriter: Writer[GeoTiff[MultibandTile]] = geowaveDataStore.createWriter(dataTypeAdapter.getTypeName)
        try indexWriter.write(tile) finally if (indexWriter != null) indexWriter.close()
        tile
      } }.parSequence }).unsafeRunSync()

    val session = SessionPool.getInstance().getSession(cassandra)
    val cluster = session.getCluster
    session.close()
    cluster.close()
    BlockingThreadPool.pool.shutdown()

    println(Console.RED)
    println("-----------------------------")
    println(s"Ingested Items: ${result.length}")
    println("-----------------------------")
    println(Console.RESET)
  }
}
