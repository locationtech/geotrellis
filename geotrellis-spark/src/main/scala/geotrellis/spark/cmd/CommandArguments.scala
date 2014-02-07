package geotrellis.spark.cmd

import com.quantifind.sumac.FieldArgs
import com.quantifind.sumac.validation.Required

class CommandArguments extends FieldArgs {
  @Required var input: String = _
  @Required var output: String = _
  var sparkMaster: String = _
  
  // for debugging only
  var dumpDir: String = _
}