package geotrellis.admin

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest
import spray.http._
import akka.testkit.TestActorRef
import geotrellis.GeoTrellis
import java.io.File

/**
 * Created by jchien on 2/8/14.
 */
class AdminServiceActorSpec extends FunSpec with ScalatestRouteTest
                                            with ShouldMatchers
                                            with HttpService {
  def actorRefFactory = system
  val data = Array(1.toByte, 2.toByte, 3.toByte)
  val asa = TestActorRef(new AdminServiceActor("")).underlyingActor

  describe("AdminServiceActor") {

    it("should upload files to the correct datastore") {
      val file = new File(GeoTrellis.server.catalog.path("test:fs").get + "/" + "reservedfilename7.arg")

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
