package geotrellis.spark.etl

import geotrellis.spark.etl.config.{BackendProfile, CassandraProfile}
import geotrellis.spark.io.cassandra.CassandraInstance

package object cassandra {
  private[cassandra] def getInstance(bp: Option[BackendProfile]): CassandraInstance =
    bp.collect { case bp: CassandraProfile => bp.getInstance }.get
}
