package geotrellis.spark.cmd

import geotrellis.spark.cmd.args._
import geotrellis.spark.service.TmsHttpActor

import akka.actor.{ActorSystem, Props}
import akka.io.IO

import com.typesafe.config._

import spray.can.Http

import com.quantifind.sumac.ArgMain
import com.quantifind.sumac.validation.Required

class TmsArgs extends AccumuloArgs with SparkArgs with HadoopArgs

object TMS extends ArgMain[TmsArgs] {
  def main(args: TmsArgs) {
    val config = ConfigFactory.load()
    val host = config.getString("geotrellis.admin.host")
    val port = config.getInt("geotrellis.admin.port")

    implicit val system = ActorSystem()
    val service = system.actorOf(TmsHttpActor.props(args), "tms-service")
    IO(Http) ! Http.Bind(service, host, port)
  }
}
