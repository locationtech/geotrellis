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

import spray.routing._
import spray.http.MediaTypes

import org.apache.accumulo.core.client.security.tokens.PasswordToken

object TmsHttpActor {
  def props(args: TmsArgs): Props = akka.actor.Props(classOf[TmsHttpActor], args)
}

class TmsHttpActor(val args: TmsArgs) extends Actor with TmsHttpService {
  def actorRefFactory = context

  def receive = runRoute(rootRoute)
}

trait TmsHttpService extends HttpService {
  val args: TmsArgs
  implicit val sc = args.sparkContext("TMS Service")

  val accumulo = AccumuloInstance(
    args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
  val catalog = accumulo.catalog

  def rootRoute =
    pathPrefix("tms" / Segment / DoubleNumber / IntNumber / IntNumber / IntNumber ) { (layer, time, zoom, x , y) =>

      val rdd: Option[RasterRDD[SpatialKey]] =  ??? //catalog.load[TimeBandTile](layer, zoom, SpaceFilter(x,y,TmsCoordScheme), TimeFilter(time))

      respondWithMediaType(MediaTypes.`image/png`) { complete {
        val tile = rdd.get.first.tile
        Encoder(Settings(Rgba, PaethFilter)).writeByteArray(tile)
      } }
    }
}
