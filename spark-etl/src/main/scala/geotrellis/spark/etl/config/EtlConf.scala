package geotrellis.spark.etl.config

import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.json._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import spray.json.DefaultJsonProtocol._
import spray.json._

class EtlConf(val credentials: Credentials, val datasets: List[Input], val output: Output) {
  private lazy val credentialsOutput = credentials.getOutput(output)
  def getEtlJobs = datasets map { ds => EtlJob(ds, output, credentials.getInput(ds), credentialsOutput) }
}

object EtlConf {
  val help = """
               |geotrellis-etl
               |
               |Usage: geotrellis-etl [options]
               |
               |  --datasets <value>
               |        datasets is a non-empty String property
               |  --output <value>
               |        output is a non-empty String property
               |  --credentials <value>
               |        credentials is a non-empty String property
               |  --help
               |        prints this usage text
             """.stripMargin

  val requiredFields = Set('datasets, 'output, 'credentials)

  val schemaFactory     = JsonSchemaFactory.byDefault()
  val credentialsSchema = schemaFactory.getJsonSchema(JsonLoader.fromResource("/credentials-schema.json"))
  val inputSchema       = schemaFactory.getJsonSchema(JsonLoader.fromResource("/input-schema.json"))
  val outputSchema      = schemaFactory.getJsonSchema(JsonLoader.fromResource("/output-schema.json"))

  def getJson(filePath: String, conf: Configuration): String = {
    val path = new Path(filePath)
    val fs = path.getFileSystem(conf)
    val is = fs.open(path)
    val json = scala.io.Source.fromInputStream(is).getLines.mkString(" ")
    is.close(); fs.close(); json
  }

  def nextOption(map: Map[Symbol, String], list: Seq[String]): Map[Symbol, String] =
    list.toList match {
      case Nil => map
      case "--datasets" :: value :: tail =>
        nextOption(map ++ Map('datasets -> value), tail)
      case "--credentials" :: value :: tail =>
        nextOption(map ++ Map('credentials -> value), tail)
      case "--output" :: value :: tail =>
        nextOption(map ++ Map('output -> value), tail)
      case "--help" :: tail => {
        println(help)
        sys.exit(1)
      }
      case option :: tail => {
        println(s"Unknown option ${option}")
        println(help)
        sys.exit(1)
      }
    }

  def parse(args: Seq[String])(implicit sc: SparkContext) =
    nextOption(Map(), args).map { case (key, value) => key -> getJson(value, sc.hadoopConfiguration) }

  def apply(args: Seq[String])(implicit sc: SparkContext) = {
    val m = parse(args)

    if(m.keySet != requiredFields) {
      println(s"missing required field(s): ${(requiredFields -- m.keySet).mkString(", ")}, use --help command to get additional information about input options.")
      sys.exit(1)
    }

    val(credentials, datasets, output) = (m('credentials), m('datasets), m('output))

    val inputValidation       = inputSchema.validate(JsonLoader.fromString(datasets), true)
    val credentialsValidation = credentialsSchema.validate(JsonLoader.fromString(credentials), true)
    val outputValidation      = outputSchema.validate(JsonLoader.fromString(output), true)

    if(!inputValidation.isSuccess || !credentialsValidation.isSuccess || !outputValidation.isSuccess) {
      if(!inputValidation.isSuccess) {
        println("datasets validation error:")
        println(inputValidation)
      }
      if(!credentialsValidation.isSuccess) {
        println("credentials validation error:")
        println(credentialsValidation)
      }
      if(!outputValidation.isSuccess) {
        println("output validation error:")
        println(outputValidation)
      }
      sys.exit(1)
    }

    new EtlConf(credentials.parseJson.convertTo[Credentials], datasets.parseJson.convertTo[List[Input]], output.parseJson.convertTo[Output])
  }
}
