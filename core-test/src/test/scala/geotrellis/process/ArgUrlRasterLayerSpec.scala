/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.process

import org.scalatest._

import scala.math.abs

import geotrellis._
import geotrellis.data.arg._
import geotrellis.testkit._
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
                               with Matchers 
                               with TestServer 
                               with RasterBuilders 
                               with BeforeAndAfterAll {
    def actor(routingSettings:RoutingSettings) = 
      new HttpServiceActor {
        implicit def rs = routingSettings

        def receive = runRoute {
          get {
            path("criml-url.json") {
              getFromFile("core-test/data/criml-url.json")
            } ~
            path("criml+url.json") {
              getFromFile("core-test/data/criml-url.json")
            } ~
            path("criml-url.arg") {
              getFromFile("core-test/data/criml-url.arg")
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
    val path1 = "core-test/data/criml-url.json"
    	 
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
