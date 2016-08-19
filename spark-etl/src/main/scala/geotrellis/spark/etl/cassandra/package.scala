package geotrellis.spark.etl

import geotrellis.spark.etl.config.{Backend, BackendProfile, CassandraPath, CassandraProfile}
import geotrellis.spark.io.cassandra.CassandraInstance

package object cassandra {
  private[cassandra] def getInstance(bp: Option[BackendProfile]): CassandraInstance =
    bp.collect { case bp: CassandraProfile => bp.getInstance }.get

  def getPath(b: Backend): CassandraPath =
    b.path match {
      case p: CassandraPath => p
      case _ => throw new Exception("Path string not corresponds backend type")
    }
}
