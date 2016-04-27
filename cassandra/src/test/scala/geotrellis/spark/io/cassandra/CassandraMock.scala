package geotrellis.spark.io.cassandra

import org.cassandraunit.utils.EmbeddedCassandraServerHelper

object CassandraMock {
  lazy val start = try {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra.yaml")
  } catch {
    case e: ExceptionInInitializerError => println("already init")
  }

  def withBaseCassandraInstance[K](hosts: Seq[String],
                                   keyspace: String,
                                   username: String = "",
                                   password: String = "",
                                   replicationStrategy: String = "SimpleStrategy",
                                   replicationFactor: Int = 1)(block: BaseCassandraInstance => K): K = {
    start
    block(BaseCassandraInstance(hosts, keyspace, username, password, replicationStrategy, replicationFactor))
  }
  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     keyspace: String,
                                     username: String = "",
                                     password: String = "",
                                     replicationStrategy: String = "SimpleStrategy",
                                     replicationFactor: Int = 1)(block: BaseCassandraInstance => K): K = {
    start
    val instance = BaseCassandraInstance(hosts, keyspace, username, password, replicationStrategy, replicationFactor)
    try block(instance) finally instance.closeAsync
  }
}
