package geotrellis.spark.io.hbase

import geotrellis.spark._

object HBaseKeyEncoder {
  def encode(id: LayerId, index: Long, trailingByte: Boolean = false): Array[Byte] = {
    val result: Array[Byte] = (s"${HBaseRDDWriter.layerIdString(id)}": Array[Byte]) ++ (index: Array[Byte])
    if(trailingByte) result :+ 0.toByte else result
  }
}
