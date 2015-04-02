package geotrellis.spark.cmd.args

import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required

trait CassandraArgs extends FieldArgs {
  @Required var host: String = _
  @Required var keyspace: String = _
}
