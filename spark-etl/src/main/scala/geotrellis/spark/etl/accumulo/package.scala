package geotrellis.spark.etl

import geotrellis.spark.etl.config.{AccumuloPath, AccumuloProfile, Backend, BackendProfile}
import geotrellis.spark.io.accumulo.AccumuloInstance

package object accumulo {
  private[accumulo] def getInstance(bp: Option[BackendProfile]): AccumuloInstance =
    bp.collect { case ap: AccumuloProfile => ap.getInstance }.get

  def getPath(b: Backend): AccumuloPath =
    b.path match {
      case p: AccumuloPath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
