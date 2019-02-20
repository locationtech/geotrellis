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
import java.nio.ByteBuffer

import com.datastax.driver.core.DataType.{blob, cint, text, varint}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Session}
import com.datastax.driver.core.querybuilder.{BuiltStatement, QueryBuilder}
import geotrellis.spark.io.cassandra.conf.CassandraIndexStrategy
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder.{Caching, KeyCaching}
import com.datastax.driver.core.schemabuilder.TableOptions.CachingRowsPerPartition
import com.datastax.driver.core.schemabuilder.{SchemaBuilder, SchemaStatement}
import geotrellis.spark.io.index.KeyIndex
import spire.math.Interval
import spire.implicits._

import scala.collection.immutable.VectorBuilder

protected[cassandra] class CassandraIndexing[K](
  keyIndex: KeyIndex[K],
  tilesPerPartition: Int
) extends Serializable {

  import CassandraIndexing._

  //NOTE: Computed iff CassandraRangeQueryStrategy == `Read-Optimized-Query`.
  //      Depending on the size of the keyIndex range and number of bins, this can be somewhat costly to compute.
  @transient private lazy val zoomBinIntervals: ZoomBinIntervals = {
    /**
      * NOTE: This makes an assumption that the range of a SFC index can not be updated in such a way
      * that the `indexRanges` would change without recomputing the index itself (and thus reindexing the data on disk).
      * If this is *NOT* the case then we'll need to compute these zoomBinIntervals "on the fly", which is
      * more computationally expensive and may discount any time savings we may gain from introducing the bin in the
      * first place.
      */
    val ranges = keyIndex.indexRanges(keyIndex.keyBounds)

    val binRanges = ranges.toVector.map{ range =>
      val vb = new VectorBuilder[Interval[BigInt]]()
      cfor(range._1)(_ <= range._2, _ + tilesPerPartition){ i =>
        vb += Interval.openUpper(i, i + tilesPerPartition)
      }
      vb.result()
    }

    CassandraIndexing.ZoomBinIntervals(binRanges.flatten.zipWithIndex)
  }

  /**
    * Obtain the create statement provided the index strategy.
    *
    * @param indexStrategy
    * @param keyspace
    * @param table
    * @return
    */
  def createTableStatement(
    indexStrategy: CassandraIndexStrategy,
    keyspace: String,
    table: String
  ): CreateTableStatement = {
    indexStrategy match {
      case conf.ReadOptimized =>
        CreateTableStatement(
          SchemaBuilder.createTable(keyspace, table).ifNotExists()
            .addPartitionKey("name", text)
            .addPartitionKey("zoom", cint)
            .addPartitionKey("zoombin", cint)
            .addClusteringColumn("key", varint)
            .addColumn("value", blob)
            .withOptions()
            .caching(KeyCaching.ALL, SchemaBuilder.rows(tilesPerPartition))
        )

      case conf.WriteOptimized =>
        CreateTableStatement(
          SchemaBuilder.createTable(keyspace, table).ifNotExists()
            .addPartitionKey("key", varint)
            .addClusteringColumn("name", text)
            .addClusteringColumn("zoom", cint)
            .addColumn("value", blob)
        )
    }
  }

  /**
    * Obtain the delete statement provided the index strategy.
    *
    * @param indexStrategy
    * @param keyspace
    * @param table
    * @param layerName
    * @param zoom
    * @return
    */
  def deleteStatement(
    indexStrategy: CassandraIndexStrategy,
    keyspace: String,
    table: String,
    layerName: String,
    zoom: Int
  ): DeleteStatement = {
    indexStrategy match {
      case conf.ReadOptimized =>
        DeleteStatement(
          QueryBuilder.delete()
            .from(keyspace, table)
            .where(eqs("name", layerName))
            .and(eqs("zoom", zoom))
            .and(eqs("zoombin", QueryBuilder.bindMarker()))
            .and(eqs("key", QueryBuilder.bindMarker()))
        )

      case conf.WriteOptimized =>
        DeleteStatement(
          QueryBuilder.delete()
            .from(keyspace, table)
            .where(eqs("key", QueryBuilder.bindMarker()))
            .and(eqs("name", layerName))
            .and(eqs("zoom", zoom))
        )
    }
  }

  /**
    * Obtain the query statement provided the index strategy.
    *
    * @param indexStrategy Read or Write optimized strategy
    * @param keyspace The keyspace on which to operate
    * @param table The table within the keyspace to operate on
    * @param layerName The layername to operate on
    * @param zoom The zoom level
    * @return A BuiltStatement for a query to fetch a tile
    */
  def queryValueStatement(
    indexStrategy: CassandraIndexStrategy,
    keyspace: String,
    table: String,
    layerName: String,
    zoom: Int
  ): QueryValueStatement = {
    indexStrategy match {
      case conf.ReadOptimized =>
        QueryValueStatement(
          QueryBuilder.select("value")
            .from(keyspace, table)
            .where(eqs("name", layerName))
            .and(eqs("zoom", zoom))
            .and(eqs("zoombin", QueryBuilder.bindMarker()))
            .and(eqs("key", QueryBuilder.bindMarker()))
        )

      case conf.WriteOptimized =>
        QueryValueStatement(
          QueryBuilder.select("value")
            .from(keyspace, table)
            .where(eqs("key", QueryBuilder.bindMarker()))
            .and(eqs("name", layerName))
            .and(eqs("zoom", zoom))
        )
    }
  }

  def writeValueStatement(
    indexStrategy: CassandraIndexStrategy,
    keyspace: String,
    table: String,
    layerName: String,
    zoom: Int
  ): WriteValueStatement = {
    indexStrategy match {
      case conf.ReadOptimized =>
        WriteValueStatement(
          QueryBuilder
            .insertInto(keyspace, table)
            .value("name", layerName)
            .value("zoom", zoom)
            .value("zoombin", QueryBuilder.bindMarker())
            .value("key", QueryBuilder.bindMarker())
            .value("value", QueryBuilder.bindMarker())
        )

      case conf.WriteOptimized =>
        WriteValueStatement(
          QueryBuilder
            .insertInto(keyspace, table)
            .value("name", layerName)
            .value("zoom", zoom)
            .value("key", QueryBuilder.bindMarker())
            .value("value", QueryBuilder.bindMarker())
        )
    }
  }

  private def zoomBin(
    index: BigInteger
  ): java.lang.Integer = {
    zoomBinIntervals.intervals.find{ case (interval, idx) => interval.contains(index) }.map {
      _._2: java.lang.Integer
    }.getOrElse(0: java.lang.Integer)
  }

  def prepareQuery(
    statement: CassandraStatement
  )(session: => Session): CassandraPreparedStatement = {
    statement match {
      case WriteValueStatement(s) => PreparedWriteValueStatement(session.prepare(s))
      case QueryValueStatement(s) => PreparedQueryValueStatement(session.prepare(s))
      case DeleteStatement(s) => PreparedDeleteStatement(session.prepare(s))
    }
  }

  def bindQuery(
    queryStrategy: CassandraIndexStrategy,
    statement: CassandraPreparedStatement,
    index: BigInteger,
    value: Option[ByteBuffer] = None
  ): BoundStatement = {
    queryStrategy match {
      case conf.ReadOptimized =>
        statement match{
          case PreparedQueryValueStatement(p) => p.bind(zoomBin(index), index)
          case PreparedDeleteStatement(p) => p.bind(zoomBin(index), index)
          case PreparedWriteValueStatement(p) =>
            //TODO: Is there a better way to enforce this than at runtime?
            require(value.nonEmpty, "Cannot bind write statement with empty value")
            p.bind(zoomBin(index), index, value.get)
        }
      case conf.WriteOptimized =>
        statement match {
          case PreparedQueryValueStatement(p) => p.bind(index)
          case PreparedDeleteStatement(p) => p.bind(index)
          case PreparedWriteValueStatement(p) =>
            require(value.nonEmpty, "Cannot bind write statement with empty value")
            p.bind(index, value.get)
        }
    }
  }

}

object CassandraIndexing{
  sealed trait CassandraStatement extends Any
  sealed trait CassandraPreparedStatement extends Any

  //Value Classes - should be unwrapped at compile time:
  case class ZoomBinIntervals(intervals: Vector[(Interval[BigInt], Int)]) extends AnyVal

  final case class WriteValueStatement(statement: BuiltStatement) extends AnyVal with CassandraStatement
  final case class QueryValueStatement(statement: BuiltStatement) extends AnyVal with CassandraStatement
  final case class DeleteStatement(statement: BuiltStatement) extends AnyVal with CassandraStatement
  final case class CreateTableStatement(statement: SchemaStatement) extends AnyVal

  final case class PreparedWriteValueStatement(statement: PreparedStatement) extends AnyVal with CassandraPreparedStatement
  final case class PreparedQueryValueStatement(statement: PreparedStatement) extends AnyVal with CassandraPreparedStatement
  final case class PreparedDeleteStatement(statement: PreparedStatement) extends AnyVal with CassandraPreparedStatement
}
