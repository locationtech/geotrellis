package geotrellis.spark.io.accumulo

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io.{AttributeStore, LayerDeleter}
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.security.Authorizations
import spray.json.JsonFormat
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.hadoop.io.Text
import geotrellis.spark.io.json._
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

class AccumuloLayerDeleter[K: Boundable: JsonFormat: ClassTag, V]
  (val attributeStore: AttributeStore[JsonFormat], connector: Connector) extends LayerDeleter[LayerId]{

  lazy val ops = connector.tableOperations()

  def delete(id: LayerId): Unit = {
    val (header, _, keyBounds, keyIndex, _) =
      attributeStore.readLayerAttributes[AccumuloLayerHeader, Unit, KeyBounds[K], KeyIndex[K], Unit](id)

    def decompose(bounds: KeyBounds[K], keyIndex: KeyIndex[K]) =
      keyIndex.indexRanges(bounds).map { case (min, max) =>
        new AccumuloRange(new Text(long2Bytes(min)), new Text(long2Bytes(max)))
      }

    val ranges = decompose(keyBounds, keyIndex)
    val numThreads = 1
    val config = new BatchWriterConfig()
    config.setMaxWriteThreads(numThreads)
    val deleter = connector.createBatchDeleter(header.tileTable, new Authorizations(), numThreads, config)
    deleter.fetchColumnFamily(columnFamily(id))
    deleter.setRanges(ranges)
    deleter.delete()

    attributeStore.delete(id)
  }
}


