package geotrellis.spark.etl

import geotrellis.spark.io.hbase.HBaseInstance

package object hbase {

  private[hbase] def getInstance(props: Map[String, String]): HBaseInstance =
    HBaseInstance(props("zookeepers").split(","), props("master"))

}
