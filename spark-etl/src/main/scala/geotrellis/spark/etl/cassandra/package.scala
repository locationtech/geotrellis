package geotrellis.spark.etl

import geotrellis.spark.etl.config.backend.{Backend, Cassandra}
import geotrellis.spark.io.cassandra.{BaseCassandraInstance, CassandraInstance}

package object cassandra {
  private[cassandra] def getInstance(credentials: Option[Backend]): CassandraInstance =
    credentials.collect { case credentials: Cassandra =>
      BaseCassandraInstance(
        credentials.hosts.split(","),
        credentials.user,
        credentials.password,
        credentials.replicationStrategy,
        credentials.replicationFactor,
        credentials.localDc,
        credentials.usedHostsPerRemoteDc,
        credentials.allowRemoteDCsForLocalConsistencyLevel
      )
    }.get
}
