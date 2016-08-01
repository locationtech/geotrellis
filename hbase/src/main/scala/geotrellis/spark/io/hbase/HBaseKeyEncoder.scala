package geotrellis.spark.io.hbase

import geotrellis.spark._

object HBaseKeyEncoder {
  def encode(id: LayerId, index: Long): Array[Byte] = (s"${HBaseRDDWriter.layerIdString(id)}": Array[Byte]) ++ (index: Array[Byte])
}
