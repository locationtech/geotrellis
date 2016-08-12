package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Backend, BackendProfile, HBasePath, HBaseProfile}
import geotrellis.spark.io.hbase.HBaseInstance

package object hbase {
  private[hbase] def getInstance(bp: Option[BackendProfile]): HBaseInstance =
    bp.collect { case ap: HBaseProfile => ap.getInstance }.get

  def getPath(b: Backend): HBasePath =
    b.path match {
      case p: HBasePath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
