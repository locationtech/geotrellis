package geotrellis.spark.cmd.args

import com.quantifind.sumac.FieldArgs
import geotrellis.spark.utils.SparkUtils

trait HadoopArgs extends FieldArgs with ArgsParser {
  var hadoopOpts: String = _
  
  val hadoopConf = {
    val hadoopConf = SparkUtils.createHadoopConfiguration
    if (hadoopOpts != null) {
      val hadoopArgs = parseArgs(hadoopOpts)
      hadoopArgs.foreach { case (k, v) => println(s"key=${k}, val=${v}") }
      hadoopArgs.foreach { case (k, v) => hadoopConf.set(k, v) }
    }
    hadoopConf
  }
}