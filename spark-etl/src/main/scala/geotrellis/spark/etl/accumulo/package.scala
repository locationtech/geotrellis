package geotrellis.spark.etl

import geotrellis.spark.io.accumulo.AccumuloInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken

package object accumulo {

  private[accumulo] def getInstance(props: Map[String, String]): AccumuloInstance =
    AccumuloInstance(props("instance"), props("zookeeper"), props("user"), new PasswordToken(props("password")))

}
