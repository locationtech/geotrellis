package geotrellis.spark.cmd

import akka.actor.ActorSystem
import com.quantifind.sumac.{ArgApp, ArgMain}
import geotrellis.proj4.LatLng
import geotrellis.raster.GridBounds
import geotrellis.spark._
import geotrellis.spark.cmd.args.{AccumuloArgs, SparkArgs, HadoopArgs}
import geotrellis.spark.io.accumulo.{Layer, AccumuloInstance}
import geotrellis.spark.tiling.{GridCoordScheme, TmsCoordScheme}
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.joda.time.DateTime
import spray.http.{AllOrigins, MediaTypes}
import spray.httpx.SprayJsonSupport
import spray.routing.SimpleRoutingApp
import geotrellis.raster.render._
import geotrellis.vector.reproject._
import scala.concurrent._
import ExecutionContext.Implicits.global

import spray.json._
import geotrellis.spark.json._




import spray.http.{HttpMethods, HttpMethod, HttpResponse, AllOrigins}
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.routing._

trait CORSSupport { self: HttpService =>
  val corsHeaders = List(`Access-Control-Allow-Origin`(AllOrigins),
    `Access-Control-Allow-Methods`(GET, POST, OPTIONS, DELETE),
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent"))

  def cors: Directive0 = {
    val rh = implicitly[RejectionHandler]
    respondWithHeaders(corsHeaders) & handleRejections(rh)
  }
}

class TmsArgs extends SparkArgs with AccumuloArgs with HadoopArgs

/**
 * Catalog and TMS service for TimeRaster layers only
 * This is intended to exercise the machinery more than being a serious service.
 */
object CatalogService extends ArgApp[TmsArgs] with SimpleRoutingApp with SprayJsonSupport with CORSSupport {
  implicit val system = ActorSystem("spray-system")
  implicit val sc = argHolder.sparkContext("Catalog Service")  // for geotrellis

  val accumulo = AccumuloInstance(argHolder.instance, argHolder.zookeeper,
    argHolder.user, new PasswordToken(argHolder.password))
  val catalog = accumulo.catalog

  /** Simple route to test responsiveness of service. */
  val pingPong = path("ping")(complete("pong"))

  /** Server out TMS tiles for some layer */
  def tmsRoute =
    pathPrefix(Segment / IntNumber / IntNumber / IntNumber ) { (layer, zoom, x , y) =>
      parameters('time) { time =>
        val dt = DateTime.parse(time)
        println(layer, zoom, x, y, dt)

        val rdd = catalog.load[TimeTileId](layer, zoom, FilterSet[TimeTileId]()
          withFilter SpaceFilter(GridBounds(x, y, x, y), TmsCoordScheme)
          withFilter TimeFilter(dt, dt))

        respondWithMediaType(MediaTypes.`image/png`) {
          complete {
            future {
              rdd.get.first().tile.renderPng().bytes
            }
          }
        }
      }
    }

  def catalogRoute = cors {
    path("") {
      get {
        // get the entire catalog
        complete {
          import DefaultJsonProtocol._
          accumulo.metaDataCatalog.fetchAll.mapValues(_._2).toSeq.map {
            case (layer, md) =>
              val center = md.extent.reproject(md.crs, LatLng).center
              JsObject("layer" -> layer.toJson, "metaData" -> md.toJson, "center" -> List(center.x, center.y).toJson)
          }
        }
      }
    } ~
    pathPrefix(Segment / IntNumber / "bands") {
      (layer, zoom) =>
        get {
          complete {
            import DefaultJsonProtocol._

            accumulo.metaDataCatalog.get(Layer(layer, zoom)).flatMap {
              case (table, md) =>
                val GridBounds(col, row, _, _) = md.gridBounds
                val filters = new FilterSet[TimeTileId]() withFilter SpaceFilter(GridBounds(col, row, col, row), GridCoordScheme)
                catalog.load(layer, zoom, filters).map(_.map {
                  case (key, tile) => key.time.toString
                })
            }.get.collect
          }
        }
    }
  }
  def root = {
    pathPrefix("catalog") { catalogRoute } ~
    pathPrefix("tms") { tmsRoute }
  }
  
  startServer(interface = "localhost", port = 8080) {
    get(pingPong ~  root)
  }
}