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

package geotrellis.spark.store.hbase

import geotrellis.layers.LayerId
import geotrellis.store.hbase._
import geotrellis.spark.io.LayerWriter
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.spark.util.KryoWrapper
import org.apache.avro.Schema
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter._
import org.apache.hadoop.hbase.{CompareOperator, TableName}
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._

object HBaseRDDWriter {
  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: HBaseInstance,
    layerId: LayerId,
    decomposeKey: K => BigInt,
    table: String
  ): Unit = update(raster, instance, layerId, decomposeKey, table, None, None)

  private[hbase] def update[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: HBaseInstance,
    layerId: LayerId,
    decomposeKey: K => BigInt,
    table: String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V,V) => V]
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec = KeyValueRecordCodec[K, V]

    // create tile table if it does not exist
    instance.withAdminDo { admin =>
      if (!admin.tableExists(table)) {
        val idsColumnFamilyDesc = ColumnFamilyDescriptorBuilder.of(hbaseTileColumnFamily)
        val tableDesc = TableDescriptorBuilder.newBuilder(table: TableName).setColumnFamily(idsColumnFamilyDesc).build()
        admin.createTable(tableDesc)
      }
    }

    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    // Call groupBy with numPartitions; if called without that argument or a partitioner,
    // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
    // on a key type that may no longer by valid for the key type of the resulting RDD.
    raster.groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
      .foreachPartition { partition: Iterator[(BigInt, Iterable[(K, V)])] =>
        if(partition.nonEmpty) {
          instance.withConnectionDo { connection =>
            val mutator = connection.getBufferedMutator(table)
            val tableConnection = connection.getTable(table)

            partition.foreach { recs =>
              val id = recs._1
              val current = recs._2.toVector
              val updated = LayerWriter.updateRecords(mergeFunc, current, existing = {
                val scan = new Scan()
                scan.addFamily(hbaseTileColumnFamily)
                val filter = new FilterList(
                  new PrefixFilter(hbaseLayerIdString(layerId)),
                  new RowFilter(CompareOperator.EQUAL, new BinaryComparator(HBaseKeyEncoder.encode(layerId, id))))
                scan.setFilter(filter)
                val scanner = tableConnection.getScanner(scan)
                val results: Vector[(K,V)] = scanner.iterator.asScala.toVector.flatMap{ result =>
                  val bytes = result.getValue(hbaseTileColumnFamily, "")
                  val schema = kwWriterSchema.value.getOrElse(_recordCodec.schema)
                  AvroEncoder.fromBinary(schema, bytes)(_recordCodec)
                }
                scanner.close()
                results
              })

              val bytes = AvroEncoder.toBinary(updated)(codec)
              val put = new Put(HBaseKeyEncoder.encode(layerId, id))
              put.addColumn(hbaseTileColumnFamily, "", System.currentTimeMillis(), bytes)
              mutator.mutate(put)
            }

            tableConnection.close()
            mutator.flush()
            mutator.close()
          }
        }
      }
  }
}
