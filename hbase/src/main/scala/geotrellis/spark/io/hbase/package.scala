package geotrellis.spark.io

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.util.Bytes

package object hbase {
  implicit def stringToTableName(str: String): TableName = TableName.valueOf(str)
  implicit def stringToBytes(str: String): Array[Byte]  = Bytes.toBytes(str)
}
