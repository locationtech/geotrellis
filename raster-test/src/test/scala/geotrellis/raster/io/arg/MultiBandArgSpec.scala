package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class MultiBandArgSpec extends FunSpec
                       with TestEngine 
                       with Matchers {
   describe("MultiBandArgReader and MultiBandArgWriter") {
     
     it("MultiBandArgReader check"){
        val fromRasterSource = RasterSource("SBN_inc_percap").get
        val extent = Extent(-8475497.88485957, 4825540.69147447, -8317922.884859569, 4954765.69147447)
        
        MultibandArgWriter(TypeByte).write("raster-test/data/sbn/MultiBand_SBN_inc_percap.json", MultiBandTile(Array(fromRasterSource)), extent, "MultiBand_SBN_inc_percap")
        val fromArgReader = MultibandArgReader.read("raster-test/data/sbn/MultiBand_SBN_inc_percap.json")
        
        assertEqual(fromArgReader.getBand(0), fromRasterSource) 
     }
     
     it("should write MultiBandTile and match with Read MultiBandTile"){
       val extent = Extent(10.0, 11.0, 14.0, 15.0)
       
       val array1 = Array(NODATA, -1, 2, -3,
    		   			4, -5, 6, -7,
    		   			8, -9, 10, -11,
    		   			12, -13, 14, -15)

       val tile1 = IntArrayTile(array1, 4, 4)
  
       val array2 = Array(NODATA, 4, -5, 6,
    		   			-1, 2, -3, -7,
    		   			12, -13, 14, -15,
    		   			8, -9, 10, -11)
    
       val tile2 = IntArrayTile(array2, 4, 4)
       
       val array3 = Array(NODATA, NODATA, 2, -3,
    		   			4, -5, 6, -7,
    		   			8, NODATA, 10, NODATA,
    		   			12, -13, 14, -15)
    		   			
       val tile3 = IntArrayTile(array3, 4, 4)
       
       val array4 = Array(10, -1, 2, -3,
    		   			4, -5, 6, -7,
    		   			8, -9, 10, -11,
    		   			12, -13, 14, -15)
    		   			
       val tile4 = IntArrayTile(array4, 4, 4)
       
       val multiBandTile = MultiBandTile(Array(tile1,tile2,tile3,tile4))
       
       MultibandArgWriter(TypeInt).write("raster-test/data/multiband-data/test-multibandtile.json", multiBandTile, extent, "test-multibandtile")
       
       val fromArgReader: MultiBandTile = MultibandArgReader.read("raster-test/data/multiband-data/test-multibandtile.json")
       
       assertEqual(fromArgReader.getBand(0), multiBandTile.getBand(0))
       assertEqual(fromArgReader.getBand(1), multiBandTile.getBand(1))
       assertEqual(fromArgReader.getBand(2), multiBandTile.getBand(2))
       assertEqual(fromArgReader.getBand(3), multiBandTile.getBand(3))
     }
   }
   
}