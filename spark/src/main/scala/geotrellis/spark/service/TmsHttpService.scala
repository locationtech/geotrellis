package geotrellis.spark.service

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.render.png._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
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
  val sc = args.sparkContext("TMS Service")

  val accumulo = AccumuloInstance("gis", "localhost", "root", new PasswordToken("secret"))
  sc.setZooKeeperInstance("gis", "localhost")
  sc.setAccumuloCredential("root", new PasswordToken("secret"))
  implicit val format = new TmsTilingAccumuloFormat

  def rootRoute =
    pathPrefix("tms" / Segment / IntNumber / IntNumber / IntNumber ) { (layer, zoom, x , y) =>
      val rdd1 = sc.accumuloRDD(accumulo.connector)(
        "tiles",  TmsLayer(layer, zoom), Some(GridBounds(x, y, x, y), TmsCoordScheme))

      val rdd2 = sc.accumuloRDD(accumulo.connector)(
        "tiles",  TmsLayer(layer, zoom), Some(GridBounds(x, y, x, y), TmsCoordScheme))

      val out = rdd1.combineTiles(rdd2){case (tms1, tms2) =>
        require(tms1.id == tms2.id)
        val res = tms1.tile.localAdd(tms2.tile)
        TmsTile(tms1.id, res)
      }

      respondWithMediaType(MediaTypes.`image/png`) { complete {
        val tile = out.first.tile
        //at least in the local case it is faster to do collect then encode
        Encoder(Settings(Rgba, PaethFilter)).writeByteArray(tile)
      } }
    }
}

