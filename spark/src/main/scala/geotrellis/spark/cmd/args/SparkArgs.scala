package geotrellis.spark.cmd.args

import com.quantifind.sumac.FieldArgs
import geotrellis.spark.utils.SparkUtils

trait SparkArgs extends FieldArgs with ArgsParser {
  var sparkMaster: String = _
  var sparkOpts: String = _
  
  def sparkContext(appName: String) = {
    val sparkConf = SparkUtils.createSparkConf
    if (sparkOpts != null) {
      val sparkArgs = parseArgs(sparkOpts)
      sparkArgs.foreach { case (k, v) => println(s"key=${k}, val=${v}") }
      sparkArgs.foreach { case (k, v) => sparkConf.set(k, v) }
    }
    SparkUtils.createSparkContext(sparkMaster, appName, sparkConf)
  }
}