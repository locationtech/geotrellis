package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Accumulo, Backend}
import geotrellis.spark.io.accumulo.AccumuloInstance

package object accumulo {
  private[accumulo] def getInstance(credentials: Option[Backend]): AccumuloInstance =
    credentials.collect { case credentials: Accumulo =>
      AccumuloInstance(
        credentials.instance,
        credentials.zookeepers,
        credentials.user,
        credentials.token
      )
    }.get
}
