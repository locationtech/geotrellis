package geotrellis.spark.etl

import geotrellis.spark.io.cassandra.{BaseCassandraInstance, CassandraInstance}

package object cassandra {

  private[cassandra] def getInstance(props: Map[String, String]): CassandraInstance =
    BaseCassandraInstance(Seq(props("host")), props("user"), props("password"))

}
