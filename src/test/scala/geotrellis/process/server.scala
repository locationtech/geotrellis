package geotrellis.process

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

import geotrellis.{Extent,RasterExtent}

class ServerSpec extends Spec with MustMatchers {

describe("A Server") {

    //it("should use caching for LoadFile") {
    //  val path = "src/test/resources/quadborder.arg32"
    //  val server = TestServer()
    //  server.enableCaching
    //
    //  val bytes1 = server.loadFile(path)
    //  val bytes2 = server.loadFile(path)
    //
    //  bytes1 must be === bytes2
    //}

    it("should use caching for LoadRaster") {
      val path = "src/test/resources/quadborder8.arg"
      val server = TestServer()
      //server.enableCaching

      //<METADATA>
      //  <VERSION value="NORMALIZED" />
      //  <ID value="" />
      //  <NAME value="quadborder" />
      //  <DESCRIPTION value="This is four quadrants with a boreder of nulls" />
      //  <SRID value="102100" />
      //  <CELLWIDTH value="8.0" />
      //  <CELLHEIGHT value="8.0" />
      //  <ORIGIN xMin="-9.5" yMin="3.8" />
      //  <DIMENSIONS width="20" height="20" />
      //  <CREATEDATE value="10/1/2010 10:35:35 AM" />
      //  <UPDATEDATE value="10/1/2010 10:35:35 AM" />
      //</METADATA>
      val geo = RasterExtent(Extent(-9.5, 3.8, 150.5, 163.8), 8.0, 8.0, 20, 20)

      //val r1 = server.loadRaster(path, geo)
      //val r2 = server.loadRaster(path, geo)

      val r1 = server.loadRaster(path, geo)
      val r2 = server.loadRaster(path, geo)

      r1 must be === r2
    }
  }

}
