package geotrellis.spark.io.hbase.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig

case class HBaseConfig(catalog: String)

object HBaseConfig extends CamelCaseConfig {
  lazy val conf: HBaseConfig = pureconfig.loadConfigOrThrow[HBaseConfig]("geotrellis.hbase")
  implicit def hbaseConfigToClass(obj: HBaseConfig.type): HBaseConfig = conf
}
