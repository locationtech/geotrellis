package geotrellis.spark.ingest

import  geotrellis.spark.cmd.args._
import com.quantifind.sumac.validation.Required

trait IngestArgs extends SparkArgs with HadoopArgs {
  @Required var input: String = _
}