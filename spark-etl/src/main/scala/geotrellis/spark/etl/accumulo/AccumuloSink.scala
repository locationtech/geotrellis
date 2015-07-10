package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.SinkPlugin
import geotrellis.spark.io.accumulo.AccumuloInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken

trait AccumuloSink extends SinkPlugin {
  val name = "accumulo"
  val requiredKeys = Array("instance", "zookeeper", "user", "password", "table")

  def getInstance(props: Map[String, String]): AccumuloInstance =
    AccumuloInstance(props("instance"), props("zookeeper"), props("user"), new PasswordToken(props("password")))
}
