package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import org.apache.cassandra.hadoop.ConfigHelper
import org.apache.cassandra.locator.{AbstractReplicationStrategy, SimpleStrategy}
import org.apache.hadoop.mapreduce.Job


trait CassandraInstance extends Serializable {
  val host: String
  val port: String

  val username: String
  val password: String

  // column family is a table name
  val keySpace: String

  // probably there is a more conviniet way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  def setCassandraConfig(job: Job) = {
    ConfigHelper.setInputInitialAddress(job.getConfiguration, host)
    ConfigHelper.setInputRpcPort(job.getConfiguration, port)
    ConfigHelper.setOutputInitialAddress(job.getConfiguration, host)
    ConfigHelper.setOutputRpcPort(job.getConfiguration, port)
    if(username.nonEmpty && password.nonEmpty) {
      ConfigHelper.setInputKeyspaceUserNameAndPassword(job.getConfiguration, username, password)
      ConfigHelper.setOutputKeyspaceUserNameAndPassword(job.getConfiguration, username, password)
    }
  }

  def setInputColumnFamily(job: Job, columnFamily: String) =
    ConfigHelper.setInputColumnFamily(job.getConfiguration, keySpace, columnFamily)

  def setOutputColumnFamily(job: Job, columnFamily: String) =
    ConfigHelper.setOutputColumnFamily(job.getConfiguration, keySpace, columnFamily)

  lazy val session = Cluster.builder().addContactPoint(host).build().connect()

  def ensureKeySpaceExists: Unit =
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${keySpace} WITH REPLICATION = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }").all()

}

case class BaseCassandraInstance(
  host: String,
  port: String,
  keySpace: String,
  username: String = "",
  password: String = "",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1) extends CassandraInstance
