package geotrellis.spark.etl

import geotrellis.spark.etl.config.{AccumuloProfile, BackendProfile}
import geotrellis.spark.io.accumulo.AccumuloInstance

package object accumulo {
  private[accumulo] def getInstance(bp: Option[BackendProfile]): AccumuloInstance =
    bp.collect { case ap: AccumuloProfile => ap.getInstance }.get
}
