package geotrellis.spark.cmd.args

import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required

trait AccumuloArgs extends FieldArgs with ArgsParser {
  @Required var zookeeper: String = _
  @Required var instance: String = _
  @Required var user: String = _
  @Required var password: String = _
}