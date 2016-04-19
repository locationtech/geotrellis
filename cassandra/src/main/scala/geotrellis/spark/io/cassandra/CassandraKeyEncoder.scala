package geotrellis.spark.io.cassandra

import geotrellis.spark.LayerId

import org.apache.cassandra.db.marshal.{AbstractType, CompositeType, IntegerType, LongType, UTF8Type}
import org.apache.cassandra.utils.ByteBufferUtil

import java.nio.ByteBuffer
import java.util

/**
  * Created by daunnc on 19/04/16.
  */
object CassandraKeyEncoder {
  def encode(id: LayerId, index: Long): ByteBuffer = {
    val keyTypes = new util.ArrayList[AbstractType[_]]()
    keyTypes.add(LongType.instance)    // key
    keyTypes.add(UTF8Type.instance)    // name
    keyTypes.add(IntegerType.instance) // zoom
    val compositeKey = CompositeType.getInstance(keyTypes)

    val builder = new CompositeType.Builder(compositeKey)
    builder.add(ByteBufferUtil.bytes(index))
    builder.add(ByteBufferUtil.bytes(id.name))
    builder.add(ByteBufferUtil.bytes(id.zoom))

    builder.build()
  }
}
