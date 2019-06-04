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

package geotrellis.spark.store.hadoop

import geotrellis.layers.LayerId
import geotrellis.layers.AttributeStore
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.index._
import geotrellis.layers.hadoop.formats.FilterMapFileInputFormat
import geotrellis.layers.hadoop._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.partition._
import geotrellis.spark.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.apache.spark.rdd._
import org.apache.hadoop.conf.Configuration

import scala.reflect._
import scala.collection.mutable


object HadoopRDDWriter extends LazyLogging {
  /** Index innterval at which map files should store an offset into sequence file.
    * This value is picked as a compromize between in-memory footprint and IO cost of retreiving a single record.
    */
  final val DefaultIndexInterval = 4

  /**
    * When record being written would exceed the block size of the current MapFile
    * opens a new file to continue writing. This allows to split partition into block-sized
    * chunks without foreknowledge of how big it is.
    */
  class MultiMapWriter(layerPath: String, partition: Int, blockSize: Long, indexInterval: Int) {
    private var writer: MapFile.Writer = null // avoids creating a MapFile for empty partitions
    private var bytesRemaining = 0l

    private def getWriter(firstIndex: BigInt) = {
      val path = new Path(layerPath, f"part-r-${partition}%05d-${firstIndex}")
      bytesRemaining = blockSize - 16*1024 // buffer by 16BK for SEQ file overhead
      val writer =
        new MapFile.Writer(
          new Configuration,
          path.toString,
          MapFile.Writer.keyClass(classOf[BigIntWritable]),
          MapFile.Writer.valueClass(classOf[BytesWritable]),
          MapFile.Writer.compression(SequenceFile.CompressionType.NONE))
      writer.setIndexInterval(indexInterval)
      writer
    }

    def write(key: BigIntWritable, value: BytesWritable): Unit = {
      val recordSize = 8 + value.getLength
      if (writer == null) {
        writer = getWriter(BigInt(key.getBytes))
      } else if (bytesRemaining - recordSize < 0) {
        writer.close()
        writer = getWriter(BigInt(key.getBytes))
      }
      writer.append(key, value)
      bytesRemaining -= recordSize
    }

    def close(): Unit =
      if (null != writer) writer.close()
  }

  private[hadoop] def update[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    layerPath: Path,
    id: LayerId,
    as: AttributeStore,
    mergeFunc: Option[(V,V) => V],
    indexInterval: Int = DefaultIndexInterval
  ): Unit = {
    val header = as.readHeader[HadoopLayerHeader](id)
    val keyIndex = as.readKeyIndex[K](id)
    val writerSchema = as.readSchema(id)
    val codec = KeyValueRecordCodec[K, V]

    val kwWriterSchema = KryoWrapper(writerSchema)

    val conf = rdd.sparkContext.hadoopConfiguration
    val _conf = SerializableConfiguration(conf)

    val ranges: Vector[(String, BigInt, BigInt)] =
      FilterMapFileInputFormat.layerRanges(new Path(header.path), conf)
        .map({ case (path, start, end) => (path.toString, start, end) })

    val layerPathStr = layerPath.toString

    val firstIndex: BigInt = ranges.head._2
    val lastIndex: BigInt = {
      val path = ranges.last._1
      val reader = new MapFile.Reader(path, conf)
      val k = new BigIntWritable()
      val v = new BytesWritable()
      var index: BigInt = BigInt(-1)
      while (reader.next(k, v)) { index = BigInt(new java.math.BigInteger(k.getBytes)) }
      reader.close
      index
    }

    val rdd2: RDD[(BigInt, K, V)] = rdd.map({ case (k,v) =>
      val i: BigInt = keyIndex.toIndex(k)
      (i, k, v)
    })

    // Write the portion of the update that does not overlap the existing layer
    val nonOverlappers: RDD[(K,V)] =
      rdd2
        .filter({ case (i, k, v) =>
          !(firstIndex <= i && i <= lastIndex) })
        .sortBy(_._1) // Necessary?
        .map({ case (_, k, v) => (k, v) })
    write(nonOverlappers, layerPath, keyIndex, indexInterval, false)

    // Write the portion of the update that overlaps the existing layer
    val overlappers: RDD[(String, Iterable[(BigInt,K,V)])] =
      rdd2
        .filter({ case (i, k, v) =>
          firstIndex <= i && i <= lastIndex })
        .groupBy({ case (i, k, v) =>
          ranges.find({ case (_,start,end) =>
            if (end == BigInt(-1))
              start <= i
            else
              start <= i && i <= end
            }) })
        .map({ case (range, ikvs) =>
          range match {
            case Some((path, _, _)) => (path, ikvs)
            case None => throw new Exception
          } })

    overlappers
      .foreach({ case (_path, ikvs1) =>
        val ikvs2 = mutable.ListBuffer.empty[(BigInt,K,V)]

        val path = new Path(_path)
        val conf = _conf.value
        val fs = path.getFileSystem(conf)
        val blockSize = fs.getDefaultBlockSize(path)
        val reader = new MapFile.Reader(path, conf)

        // Read records from map file, delete map file
        var k = new BigIntWritable()
        var v = new BytesWritable()
        while (reader.next(k, v)) {
          val _kvs2: Vector[(K,V)] = AvroEncoder.fromBinary(kwWriterSchema.value, v.getBytes)(codec)
          ikvs2 ++= _kvs2.map({ case (k, v) => (keyIndex.toIndex(k),k,v) })
        }
        reader.close
        HdfsUtils.deletePath(path, conf)

        // Merge existing records with new records
        val ikvs =
          (mergeFunc match {
            case Some(fn) =>
              (ikvs2 ++ ikvs1)
                .groupBy({ case (_,k,_) => k })
                .map({ case (k, ikvs) =>
                  val vs = ikvs.map({ case (_,_,v) => v }).toSeq
                  val v: V = vs.tail.foldLeft(vs.head)(fn)
                  (ikvs.head._1, k, v) })
            case None =>
              (ikvs2 ++ ikvs1)
                .groupBy({ case (_,k,_) => k })
                .map({ case (k, ikvs) => (ikvs.head._1, k, ikvs.head._3) })
          }).toVector.sortBy(_._1)
        val kvs = ikvs.map({ case (_,k,v) => (k,v) })

        // Write merged records
        val writer = new MultiMapWriter(layerPathStr, 33, blockSize, indexInterval)
        for ( (index, pairs) <- GroupConsecutiveIterator(kvs.toIterator)(r => keyIndex.toIndex(r._1))) {
          writer.write(
            new BigIntWritable(index.toByteArray),
            new BytesWritable(AvroEncoder.toBinary(pairs.toVector)(codec)))
        }
        writer.close() })
  }

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    path: Path,
    keyIndex: KeyIndex[K],
    indexInterval: Int = DefaultIndexInterval,
    existenceCheck: Boolean = true
  ): Unit = {
    implicit val sc = rdd.sparkContext

    val fs = path.getFileSystem(sc.hadoopConfiguration)
    if (existenceCheck) {
      if(fs.exists(path)) throw new Exception(s"Directory already exists: $path")
    }

    val codec = KeyValueRecordCodec[K, V]
    val blockSize = fs.getDefaultBlockSize(path)
    val layerPath = path.toString

    implicit val ord: Ordering[K] = Ordering.by(keyIndex.toIndex)
    rdd
      .repartitionAndSortWithinPartitions(IndexPartitioner(keyIndex, rdd.partitions.length))
      .mapPartitionsWithIndex[Unit] { (pid, iter) =>
        val writer = new MultiMapWriter(layerPath, pid, blockSize, indexInterval)
        for ( (index, pairs) <- GroupConsecutiveIterator(iter)(r => keyIndex.toIndex(r._1))) {
          writer.write(
            new BigIntWritable(index.toByteArray),
            new BytesWritable(AvroEncoder.toBinary(pairs.toVector)(codec)))
        }
        writer.close()
        // TODO: collect statistics on written records and return those
        Iterator.empty
      }.count

    fs.createNewFile(new Path(layerPath, "_SUCCESS"))
    logger.info(s"Finished saving tiles to ${path}")
  }
}
