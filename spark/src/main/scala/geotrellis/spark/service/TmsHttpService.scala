package geotrellis.spark.service

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.render.png._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.cmd.TmsArgs

import akka.actor._
import geotrellis.spark.utils.SparkUtils
import org.joda.time.DateTime

import spray.routing._
import spray.http.MediaTypes

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import scala.util._

object TmsHttpActor {
  def props(args: TmsArgs): Props = akka.actor.Props(classOf[TmsHttpActor], args)
}

class TmsHttpActor(val args: TmsArgs) extends Actor with TmsHttpService {
  def actorRefFactory = context

  def receive = runRoute(rootRoute)
}

trait TmsHttpService extends HttpService {
  val args: TmsArgs
  implicit val sc = SparkUtils.createSparkContext("TMS Service")

  val accumulo = AccumuloInstance(
    args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
  val catalog = accumulo.catalog

  def rootRoute =
    pathPrefix("tms" / Segment / Segment / IntNumber / IntNumber / IntNumber ) { (layer, timeStr, zoom, x , y) =>
      val time = DateTime.parse(timeStr)
      val tile = catalog.loadTile(LayerId(layer, zoom), SpaceTimeKey(x, y, time))

      respondWithMediaType(MediaTypes.`image/png`) { 
        complete {
          Encoder(Settings(Rgba, PaethFilter)).writeByteArray(tile)
        } 
      }
    }
}
