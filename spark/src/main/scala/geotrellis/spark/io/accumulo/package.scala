package geotrellis.spark.io

import org.apache.hadoop.io.Text
import org.apache.spark._

import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.client.{Scanner, BatchWriterConfig, Connector}
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.data.{Range => ARange, Mutation, Key, Value}
import org.apache.accumulo.core.client.mapreduce.lib.util.{ConfiguratorBase => CB}

import scala.collection.JavaConversions._

package object accumulo {
  implicit def stringToText(s: String) = new Text(s)

  implicit val rasterAccumuloDriver = RasterAccumuloDriver
  implicit val timeRasterAccumuloDriver = TimeRasterAccumuloDriver

  implicit class scannerIterator(scan: Scanner) extends Iterator[(Key, Value)] {
    val iter = scan.iterator
    override def hasNext: Boolean =
      if (iter.hasNext)
        true
      else{
        scan.close()
        false
      }

    override def next(): (Key, Value) = {
      val next = iter.next
      (next.getKey, next.getValue)
    }
  }

  trait AccumuloEncoder[T] {
    def encode(thing: T): Mutation
  }

  implicit class connectorWriter(conn: Connector) {
    def write(table: String, muts: Seq[Mutation]): Unit = {
      val cfg = new BatchWriterConfig()
      val batchWriter = conn.createBatchWriter(table, cfg)
      muts.foreach(mut => batchWriter.addMutation(mut))
      batchWriter.close()
    }
    def write(table: String, mut: Mutation): Unit =
      write(table, List(mut))

    def write[T](table: String, stuff: Seq[T])(implicit encoder: AccumuloEncoder[T]): Unit =
      write(table, stuff.map(encoder.encode))

    def write[T](table: String, thing: T)(implicit encoder: AccumuloEncoder[T]): Unit =
      write(table, List(thing))
  }
}