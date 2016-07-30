package geotrellis.spark.etl

import geotrellis.spark.etl.config.{BackendProfile, HBaseProfile}
import geotrellis.spark.io.hbase.HBaseInstance

package object hbase {
  private[hbase] def getInstance(bp: Option[BackendProfile]): HBaseInstance =
    bp.collect { case ap: HBaseProfile => ap.getInstance }.get
}
