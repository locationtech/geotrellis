package geotrellis.spark.etl

import geotrellis.spark.etl.config.backend.Credentials
import geotrellis.spark.etl.config.dataset.Config
import geotrellis.spark.etl.config.json._

import spray.json._
import spray.json.DefaultJsonProtocol._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class NewEtlConf(val credentials: Credentials, val datasets: List[Config]) {
  def getEtlJobs = datasets map { ds => EtlJob(credentials.getInput(ds), credentials.getOutput(ds), ds) }
}

object NewEtlConf {
  val help = """
               |geotrellis-etl
               |
               |Usage: geotrellis-etl [options]
               |
               |  --datasets <value>
               |        datasets is a non-empty String property
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
    list match {
      case Nil => map
      case "--dataset" :: value :: tail =>
        nextOption(map ++ Map('dataset -> value), tail)
      case "--credentials" :: value :: tail =>
        nextOption(map ++ Map('credentials -> value), tail)
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
    new NewEtlConf(m('credentials).parseJson.convertTo[Credentials], m('datasets).parseJson.convertTo[List[Config]])
  }
}
