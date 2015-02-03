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

package geotrellis.admin

import org.scalatest._
import org.scalatest.matchers._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest
import spray.http._
import akka.testkit.TestActorRef
import geotrellis.engine.GeoTrellis
import java.io.File

/**
 * This class tests some of the routing and HTTP responses of the Admin Service Actor.
 * @author jchien
 */
class AdminServiceActorSpec extends FunSpec with ScalatestRouteTest
                                            with Matchers
                                            with HttpService {
  def actorRefFactory = system
  val data = Array(1.toByte, 2.toByte, 3.toByte)
  val asa = TestActorRef(new AdminServiceActor("")).underlyingActor

  describe("AdminServiceActor") {

    it("should upload files to the correct datastore") {
      val file = new File(GeoTrellis.engine.catalog.getStore("test:fs").get.path + "/" + "reservedfilename7.arg")

      Post("/gt/upload", FormData(Seq("name" -> "reservedfilename7.arg",
        "store" -> "test:fs",
        "file" -> new String(data)))) ~> sealRoute(asa.uploadRoute) ~> check {
        status should be (StatusCodes.OK)
        file.isFile() should be (true)
      }

      file.delete()
    }

    it("should not upload files with .. or / in the name") {
      Post("/gt/upload", FormData(Seq("name" -> "../test.arg",
        "store" -> "test:fs",
        "file" -> new String(data)))) ~> sealRoute(asa.uploadRoute) ~> check {
        status should be (StatusCodes.BadRequest)
      }
    }

    it("should not upload files when invalid datastores are specified") {
      Post("/gt/upload", FormData(Seq("name" -> "test.arg",
        "store" -> "iNvalid",
        "file" -> new String(data)))) ~> sealRoute(asa.uploadRoute) ~> check {
        status should be (StatusCodes.BadRequest)
      }
    }

    it("should not upload non .arg or .json files") {
      Post("/gt/upload", FormData(Seq("name" -> "test.exe",
        "store" -> "test:fs",
        "file" -> new String(data)))) ~> sealRoute(asa.uploadRoute) ~> check {
        status should be (StatusCodes.BadRequest)
      }
    }
  }
}
