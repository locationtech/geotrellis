package geotrellis.spark.etl.config

import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

trait ConfigParse {
  val help: String
  val requiredFields: Set[Symbol]

  val schemaFactory = JsonSchemaFactory.byDefault()

  def getJson(filePath: String, conf: Configuration): String = {
    val path = new Path(filePath)
    val fs = path.getFileSystem(conf)
    val is = fs.open(path)
    val json = scala.io.Source.fromInputStream(is).getLines.mkString(" ")
    is.close(); fs.close(); json
  }

  def nextOption(map: Map[Symbol, String], list: Seq[String]): Map[Symbol, String]

  def parse(args: Seq[String])(implicit sc: SparkContext) =
    nextOption(Map(), args).map { case (key, value) => key -> getJson(value, sc.hadoopConfiguration) }
}
