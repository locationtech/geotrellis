package geotrellis.spark.service

import akka.actor._
import spray.routing._
import spray.http.MediaTypes

import geotrellis.raster.render.png._
import geotrellis.spark.tiling._
import geotrellis.spark.rdd._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.cmd.TmsArgs

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

  def rootRoute =
    pathPrefix("tms" / Segment / IntNumber / IntNumber / IntNumber ) { (layer, zoom, x , y) =>
      val pyramidPath = new Path(s"${args.root}/$layer")
      val extent = TileExtent(x,y,x,y) //this is a one tile extent
      val rdd = CroppedRasterHadoopRDD(s"$pyramidPath/$zoom",extent, sc).toRasterRDD(true)

      respondWithMediaType(MediaTypes.`image/png`) { complete {
        //at least in the local case it is faster to do collect then encode
        Encoder(Settings(Rgba, PaethFilter)).writeByteArray(rdd.first.tile)
      } }
    }
}
