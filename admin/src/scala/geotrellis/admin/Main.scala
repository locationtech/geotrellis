package pps

import geotrellis.rest.WebRunner
import geotrellis.process._
import geotrellis._
import geotrellis.source._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.feature._

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Main {
  val server = Server("asheville",
                      Catalog.fromPath("data/catalog.json"))

  def main(args: Array[String]):Unit = {
    implicit val system = server.system

    // create and start our service actor
    val service = system.actorOf(Props[ServiceActor], "pp-service")

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ! Http.Bind(service, "localhost", port = 8080)

    // val re = RasterExtent(Extent(-9001224.450862354,4070118.882129065,-8922952.933898335,4148390.3990930878),305.74811314069666,305.74811314071303,256,256)

    // server.run(io.LoadTile("den_college,0,0,Some(re)))

//    WebRunner.main(args)
  }

}

case class GetFeatureExtent(f:Op[Geometry[_]]) extends Op1(f)({
  (f) => {
    val env = f.geom.getEnvelopeInternal
    Result(Extent( env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY() ))
  }
})

case class AsPolygon[D](g:Op[Geometry[D]]) extends Op1(g) ({
  g =>
    Result(Polygon[Int](g.asInstanceOf[Polygon[D]].geom,0))
})

import java.io.File
import org.parboiled.common.FileUtils
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import spray.routing.{HttpService, RequestContext}
import spray.routing.directives.CachingDirectives
import spray.can.server.Stats
import spray.can.Http
import spray.httpx.marshalling.Marshaller
import spray.httpx.encoding.Gzip
import spray.util._
import spray.http._
import MediaTypes._
import CachingDirectives._

class ServiceActor extends Actor with PriorityPlacesService {
  def actorRefFactory = context
  def receive = runRoute(serviceRoute)
}

trait PriorityPlacesService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher

  val directoryName = "/home/rob/proj/pp/priority-places/static/"

  private def fileSystemPath(base: String, path: Uri.Path, separator: Char = File.separatorChar)(implicit log: LoggingContext): String = {
    import java.lang.StringBuilder
    def rec(p: Uri.Path, result: StringBuilder = new StringBuilder(base)): String =
      p match {
        case Uri.Path.Empty       ⇒ result.toString
        case Uri.Path.Slash(tail) ⇒ rec(tail, result.append(separator))
        case Uri.Path.Segment(head, tail) ⇒
          if (head.indexOf('/') >= 0 || head == "..") {
            log.warning("File-system path for base [{}] and Uri.Path [{}] contains suspicious path segment [{}], " +
              "GET access was disallowed", base, path, head)
            ""
          } else rec(tail, result.append(head))
      }
    rec(if (path.startsWithSlash) path.tail else path)
  }

  val serviceRoute = {
    get {
      pathSingleSlash {
        redirect("index.html",StatusCodes.Found)
      } ~
      pathPrefix("gt") {
        path("breaks") {
          parameters('bbox, 
                     'cols.as[Int],
                     'rows.as[Int],
                     'layers,
                     'weights,
                     'mask ? "",
            'numBreaks.as[Int]) {
            (bbox,cols,rows,layers,weights,mask,numBreaks) => {
              val extent = {
                val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
                Extent(xmin,ymin,xmax,ymax)
              }

              val re = RasterExtent(extent,cols,rows)

              val layerNames = layers.split(",")
              val weightValues = weights.split(",").map(_.toInt)

              val wo =
                if(layerNames.length == 1) {
                  RasterSource(layerNames(0),re) * weightValues(0)
                } else {
                  layerNames.zip(weightValues)
                    .map { case (name,weight) =>
                      RasterSource(name,re) * weight
                     }
                    .reduce(_+_)
                }

              val breaks =
                wo
                  .histogram
                  .map(_.getQuantileBreaks(numBreaks))
                  .map { breaks =>
                  val breaksArray = breaks.mkString("[", ",", "]")
                  s"""{ "classBreaks" : $breaksArray }"""
                }
              Main.server.getSource(breaks) match {
                case process.Complete(json,h) =>
                  complete(json)
                case process.Error(message,trace) =>
                  failWith(new RuntimeException(message))
              }
            }
          }
        } ~
        path("wo") {
          parameters("SERVICE",
                     'REQUEST,
                     'VERSION,
                     'FORMAT,
                     'BBOX,
                     'HEIGHT.as[Int],
                     'WIDTH.as[Int],
                     'LAYERS,
                     'WEIGHTS,
                     'PALETTE ? "ff0000,ffff00,00ff00,0000ff",
                     'COLORS.as[Int] ? 4,
                     'BREAKS,
                     'COLORRAMP ? "colorRamp",
                     'MASK ? "") {
            (_,_,_,_,bbox,cols,rows,layers,weights,palette,colors,breaks,colorRamp,mask) => {
              val extent = {
                val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
                Extent(xmin,ymin,xmax,ymax)
              }

              val re = RasterExtent(extent,cols,rows)

              val layerNames = layers.split(",")
              val weightValues = weights.split(",").map(_.toInt)

              val wo =
                if(layerNames.length == 1) {
                  RasterSource(layerNames(0),re) * weightValues(0)
                } else {
                  layerNames.zip(weightValues)
                    .map { case (name,weight) =>
                      RasterSource(name,re) * weight
                     }
                    .reduce(_+_)
                }
  
              //   val overlayOp = if(mask == "") {
              //     modelOp
              //   } else {
              //     val poly = GeoJsonReader.parse(mask)
              //     val polygon = Polygon(srs.LatLong.transform(poly.geom,srs.WebMercator),0)


              //     // val reproj = Transformer.transform(feature,Projections.LatLong,Projections.WebMercator)
              // //    val polygon = Polygon(reproj.geom,0)

              //     val maskRaster = Rasterizer.rasterizeWithValue(polygon,re) { x => 1 }
              //     local.Mask(modelOp,maskRaster,NODATA,NODATA)
              //   }
              
              val breakValues =
                breaks.split(",").map(_.toInt)
              
              val ramp = {
                val cr = services.Colors.rampMap.getOrElse(colorRamp,render.ColorRamps.BlueToRed)
                if(cr.toArray.length < breakValues.length) { cr.interpolate(breakValues.length) }
                else { cr }
              }

              val png =
                wo.renderPng(ramp,breakValues)

              //overlayOp.renderPng(ramp,breaks)//Render.operation(overlayOp,ramp,breaksOp)

              Main.server.getSource(png) match {
//              Main.server.getSource(wo.converge) match {
                case process.Complete(img,h) =>
                  respondWithMediaType(MediaTypes.`image/png`) {
//                    complete(img.toString)
                    complete(img)
                  }
                case process.Error(message,trace) =>
                  println(message)
                  println(trace)
                  println(re)

                  failWith(new RuntimeException(message))
              }
            }
          }
        }
      } ~
      pathPrefix("") {
        getFromDirectory(directoryName)
        // val base = directoryName
        // println(base)
        // unmatchedPath { path ⇒
        //   println("HERE")
        //   val p = fileSystemPath(base, path)
        //   println(p)
        //   p match {
        //     case ""       ⇒ reject
        //     case fileName ⇒ getFromFile(fileName)
        //   }
        // }
      }
    }
  }
}
