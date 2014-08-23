package geotrellis.spark.service

import geotrellis.raster._
import geotrellis.raster.render.png._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.cmd.TmsArgs

import akka.actor._

import spray.routing._
import spray.http.MediaTypes

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path

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
  sc.setZooKeeperInstance("gis", "localhost")
  sc.setAccumuloCredential("root", new PasswordToken("secret"))
  implicit val format = new TmsTilingAccumuloFormat

  def rootRoute =
    pathPrefix("tms" / Segment / IntNumber / IntNumber / IntNumber ) { (layer, zoom, x , y) =>
      val pyramidPath = new Path(s"${args.root}/$layer")
      val zoomLevel = zoom
//      val tileId = zoomLevel.tileId(x,y) //this is a one tile extent
      // TODO: FIX! This needs to go from tile coordinates -> TileId
//      val rdd = sc.accumuloRDD("tiles2", TmsLayer(layer, zoom), GridBounds(x, y, x, y))

//      val tileIds: Seq[TileId] = catalog.layer(layer, zoom).metaData.tileToIndex(TmsTileScheme, gridBounds)

      val rdd = sc.accumuloRasterRDD("tiles2", TmsLayer(layer, zoom), GridBounds(x, y, x, y), TmsCoordScheme)

      respondWithMediaType(MediaTypes.`image/png`) { complete {
        //at least in the local case it is faster to do collect then encode
        Encoder(Settings(Rgba, PaethFilter)).writeByteArray(rdd.first._2)
      } }
    }

  //  def rootRoute =
//    pathPrefix("tms" / Segment / IntNumber / IntNumber / IntNumber ) { (layer, zoom, x , y) =>
//      val pyramidPath = new Path(s"${args.root}/$layer")
//      val extent = TileExtent(x,y,x,y) //this is a one tile extent
      // val rdd = CroppedRasterHadoopRDD(s"$pyramidPath/$zoom",extent, sc).toRasterRDD
//
//      respondWithMediaType(MediaTypes.`image/png`) { complete {
//        //at least in the local case it is faster to do collect then encode
//        Encoder(Settings(Rgba, PaethFilter)).writeByteArray(rdd.first.tile)
//      } }
//    }


}

