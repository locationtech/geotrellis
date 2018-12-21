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

package geotrellis.spark.io.cassandra

import java.math.BigInteger

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io._
import com.typesafe.scalalogging.LazyLogging
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import geotrellis.spark.io.avro.AvroRecordCodec
import spire.math.Interval
import spire.implicits._
import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.collection.immutable.VectorBuilder
import scala.reflect.ClassTag

class CassandraLayerDeleter(val attributeStore: AttributeStore, instance: CassandraInstance) extends LazyLogging with LayerDeleter[LayerId] {

  def delete[K: AvroRecordCodec: Boundable: JsonFormat: ClassTag](id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[CassandraLayerHeader](id)

      val (squery, dquery) = instance.cassandraConfig.partitionStrategy match {
        case conf.`Read-Optimized-Partitioner` =>
          val squery = QueryBuilder.select("key", "zoombin")
            .from(header.keyspace, header.tileTable).allowFiltering()
            .where(eqs("name", id.name))
            .and(eqs("zoom", id.zoom))

          val dquery = QueryBuilder.delete()
            .from(header.keyspace, header.tileTable)
            .where(eqs("name", id.name))
            .and(eqs("zoom", id.zoom))
            .and(eqs("zoombin", QueryBuilder.bindMarker()))
            .and(eqs("key", QueryBuilder.bindMarker()))

          squery -> dquery

        case conf.`Write-Optimized-Partitioner` =>
          val squery = QueryBuilder.select("key")
            .from(header.keyspace, header.tileTable).allowFiltering()
            .where(eqs("name", id.name))
            .and(eqs("zoom", id.zoom))

          val dquery = QueryBuilder.delete()
            .from(header.keyspace, header.tileTable)
            .where(eqs("key", QueryBuilder.bindMarker()))
            .and(eqs("name", id.name))
            .and(eqs("zoom", id.zoom))

          squery -> dquery
      }

      lazy val intervals: Vector[(Interval[BigInt], Int)] = {
        val keyIndex = attributeStore.readKeyIndex[K](id)
        val ranges = keyIndex.indexRanges(keyIndex.keyBounds)

        val binRanges = ranges.toVector.map{ range =>
          val vb = new VectorBuilder[Interval[BigInt]]()
          cfor(range._1)(_ <= range._2, _ + instance.cassandraConfig.tilesPerPartition){ i =>
            vb += Interval.openUpper(i, i + instance.cassandraConfig.tilesPerPartition)
          }
          vb.result()
        }

        binRanges.flatten.zipWithIndex
      }

      def zoomBin(key: BigInteger): java.lang.Integer = {
        intervals.find{ case (interval, idx) => interval.contains(key) }.map {
          _._2: java.lang.Integer
        }.getOrElse(0: java.lang.Integer)
      }

      def bindQuery(statement: PreparedStatement, index: BigInteger): BoundStatement = {
        instance.cassandraConfig.partitionStrategy match {
          case conf.`Read-Optimized-Partitioner` =>
            statement.bind(zoomBin(index), index)
          case conf.`Write-Optimized-Partitioner` =>
            statement.bind(index)
        }
      }

      instance.withSessionDo { session =>
        val statement = session.prepare(dquery)

        session.execute(squery).all().asScala.map { entry =>
          session.execute(bindQuery(statement, entry.getVarint("key")))
        }
      }
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      attributeStore.delete(id)
    }
  }
}

object CassandraLayerDeleter {
  def apply(attributeStore: CassandraAttributeStore, instance: CassandraInstance): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, instance)

  def apply(attributeStore: CassandraAttributeStore): CassandraLayerDeleter =
    new CassandraLayerDeleter(attributeStore, attributeStore.instance)

  def apply(instance: CassandraInstance): CassandraLayerDeleter =
    apply(CassandraAttributeStore(instance), instance)
}
