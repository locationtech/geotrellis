package geotrellis.spark.etl.config

import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.json._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
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
    new EtlConf(m('credentials).parseJson.convertTo[Credentials], m('datasets).parseJson.convertTo[List[Input]], m('output).parseJson.convertTo[Output])
  }
}
