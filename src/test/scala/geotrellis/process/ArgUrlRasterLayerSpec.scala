package geotrellis.process

import org.scalatest._
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import scala.math.abs

import geotrellis._
import geotrellis.data.arg._
import geotrellis.testutil._
import geotrellis.raster._
import geotrellis.data._

import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.dispatch._
import spray.can.Http
import spray.http._
import spray.routing._
import StatusCodes._

class ArgUrlRasterLayerSpec extends FunSpec 
                               with MustMatchers 
                               with ShouldMatchers 
                               with TestServer 
                               with RasterBuilders 
                               with BeforeAndAfterAll {
    def actor(routingSettings:RoutingSettings) = 
      new HttpServiceActor {
        implicit def rs = routingSettings

        def receive = runRoute {
          get {
            path("criml-url.json") {
              getFromFile("src/test/resources/criml-url.json")
            } ~
            path("criml+url.json") {
              getFromFile("src/test/resources/criml-url.json")
            } ~
            path("criml-url.arg") {
              getFromFile("src/test/resources/criml-url.arg")
            } ~
            unmatchedPath { ump =>
              failWith(new RuntimeException(s"Unexpected GET $ump"))
            }
          }
        }
      }

  var system:ActorSystem = null

  override
  def beforeAll =
    try {
      super.beforeAll()
    } finally {
      system = ActorSystem("test-url-fetch")
      implicit val s = system
      implicit val timeout = Timeout(5 seconds)

      val routingSettings = RoutingSettings.default
      val service = system.actorOf(Props(actor(routingSettings)), "site-service")

      val future = IO(Http) ? Http.Bind(service, "localhost", port = 8192)
      val conf = Await.result(future, 5 seconds)
      println(conf)
    }

  
  override
  def afterAll = 
    try {
      super.afterAll()
    } finally {
      system.shutdown
    }


  describe("An ArgUrlRasterLayer") {
    val path1 = "src/test/resources/criml-url.json"
    	 
    it("should give same raster as reading directly from a file") {
      val rasterFromFile =
        RasterLayer.fromPath(path1).get.getRaster
      val rasterFromUrl =
        RasterLayer.fromUrl("http://localhost:8192/criml-url.json").get.getRaster

      assertEqual(rasterFromFile,rasterFromUrl)
    }

    it("should give same raster as reading directly from a file, with encoded url") {
      val rasterFromFile =
        RasterLayer.fromPath(path1).get.getRaster
      val rasterFromUrl =
        RasterLayer.fromUrl("http://localhost:8192/criml%2Burl.json").get.getRaster

      assertEqual(rasterFromFile,rasterFromUrl)
    }
  }
}
