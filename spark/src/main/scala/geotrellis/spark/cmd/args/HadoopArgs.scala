package geotrellis.spark.cmd.args

import com.quantifind.sumac.FieldArgs
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.Logging

trait HadoopArgs extends FieldArgs with ArgsParser with Logging {
  var hadoopOpts: String = _
  
  lazy val hadoopConf = {
    val hadoopConf = SparkUtils.createHadoopConfiguration
    
    if (hadoopOpts != null) {
      val hadoopArgs = parseArgs(hadoopOpts)
      hadoopArgs.foreach { case (k, v) => logInfo(s"key=${k}, val=${v}") }
      hadoopArgs.foreach { case (k, v) => hadoopConf.set(k, v) }
    }
    hadoopConf
  }
}