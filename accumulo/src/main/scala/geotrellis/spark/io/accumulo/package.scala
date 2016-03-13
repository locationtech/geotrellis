package geotrellis.spark.io

import geotrellis.spark.LayerId
import org.apache.hadoop.io.Text

import org.apache.accumulo.core.client.{Scanner, BatchWriterConfig, Connector}
import org.apache.accumulo.core.data.{Mutation, Key, Value}

package object accumulo {
  implicit def stringToText(s: String): Text = new Text(s)

  def columnFamily(id: LayerId) = s"${id.name}:${id.zoom}"

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
