package geotrellis.spark.cmd

import com.quantifind.sumac.FieldArgs

class CommandArguments extends FieldArgs {
  var input: String = _
  var output: String = _
  var zoom: Int = _
  var sparkMaster: String = _
  
  // for debugging only
  var dumpDir: String = _
}