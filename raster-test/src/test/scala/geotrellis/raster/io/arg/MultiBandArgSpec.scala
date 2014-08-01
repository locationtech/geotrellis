package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class MultiBandArgSpec extends FunSpec
                       with TestEngine 
                       with Matchers {
   describe("MultiBandArgReader and MultiBandWriter") {
     it("should write from RasterSource and match ArgReader with RasterSource") {
       val fromRasterSource = RasterSource("SBN_inc_percap").get
       val extent = Extent(-8475497.88485957, 4825540.69147447, -8317922.884859569, 4954765.69147447)
       
       MultibandArgWriter(TypeByte).write("raster-test/data/sbn/MultiBand_SBN_inc_percap.json", MultiBandTile(Array(fromRasterSource)), extent, "MultiBand_SBN_inc_percap")
       val fromArgReader = ArgReader.read("raster-test/data/sbn/MultiBand_SBN_inc_percap.json")
       
       assertEqual(fromArgReader, fromRasterSource)
     }
     
   }

}