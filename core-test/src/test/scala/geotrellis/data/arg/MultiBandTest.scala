/**
 * @author kelum
 *
 */

package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis._
import geotrellis.testkit._

import geotrellis.process._
import geotrellis.raster._

import org.scalatest.FunSuite

/**
 *  to run this test successfully there should be multiband-foo, multiband-foo1, multiband-foo2 and multiband-foo3 
 *  folders created in /tmp/ directory
 *
 */
class MultiBandTest extends FunSuite with TestServer{

  val array1 =   
     Array(NODATA, -1, 2, -3,
                4, -5, 6, -7,
                8, -9, 10, -11,
               12, -13, 14, -15)


  val data1 = IntArrayRasterData(array1, 4, 4)
  val e1 = Extent(10.0, 11.0, 14.0, 15.0)
  val re1 = RasterExtent(e1, 1.0, 1.0, 4, 4)
  val raster1 = Raster(data1, re1)
  val te = Extent(30.0, 31.0, 34.0, 35.0)
  val tre = RasterExtent(te,1.0,1.0,4,4)  
  
  val array2 =   
     Array(12, -13, 14, -15,
                4, -5, 6, -7,
                8, -9, 10, -11,
                30, -1, 0, 3)
  
  val data2 = IntArrayRasterData(array2, 4, 4)
  val raster2 = Raster(data2, re1)
  
  val array3 =   
     Array(NODATA, NODATA, NODATA, NODATA,
                4, -5, 6, -7,
                8, -9, 10, -11,
                NODATA, NODATA, NODATA, NODATA)
  
  val data3 = IntArrayRasterData(array3, 4, 4)
  val raster3 = Raster(data3, re1)
  
  val multibandRaster = Array(raster1, raster2, raster3) 
  
  def loadRaster(path:String) = get(io.LoadFile(path))
  
  test("write all supported arg datatypes using MultiBandArgWriter") {
    MultiBandArgWriter(TypeByte, "/tmp/multiband-foo1/multiband-foo-int8").write(multibandRaster, "multiband-foo-int8")
    MultiBandArgWriter(TypeShort, "/tmp/multiband-foo1/multiband-foo-int16").write(multibandRaster, "multiband-foo-int16")
    MultiBandArgWriter(TypeInt, "/tmp/multiband-foo1/multiband-foo-int32").write(multibandRaster, "multiband-foo-int32")
    MultiBandArgWriter(TypeFloat, "/tmp/multiband-foo1/multiband-foo-float32").write(multibandRaster, "multiband-foo-float32")
    MultiBandArgWriter(TypeDouble, "/tmp/multiband-foo1/multiband-foo-float64").write(multibandRaster, "multiband-foo-float64")
    
  }
  
  test("write all supported arg datatypes using MultiBandArgWriter2") {
    MultiBandArgWriter2(TypeByte, "/tmp/multiband-foo2/multiband-foo-int8").write(multibandRaster, "multiband-foo-int8")
    MultiBandArgWriter2(TypeShort, "/tmp/multiband-foo2/multiband-foo-int16").write(multibandRaster, "multiband-foo-int16")
    MultiBandArgWriter2(TypeInt, "/tmp/multiband-foo2/multiband-foo-int32").write(multibandRaster, "multiband-foo-int32")
    MultiBandArgWriter2(TypeFloat, "/tmp/multiband-foo2/multiband-foo-float32").write(multibandRaster, "multiband-foo-float32")
    MultiBandArgWriter2(TypeDouble, "/tmp/multiband-foo2/multiband-foo-float64").write(multibandRaster, "multiband-foo-float64")
    
  }
  
//  test("check int8") {
//    assert(loadRaster("/tmp/multiband-foo1/multiband-foo-int8-band0.arg").toArray === array1)
//    val l = loadRaster("/tmp/multiband-foo1/multiband-foo-int8-band0.arg")
//    println(l.asciiDraw)
//    
//    assert(loadRaster("/tmp/multiband-foo2/multiband-foo-int8-band0.arg").toArray === array1)
//    val m = loadRaster("/tmp/multiband-foo2/multiband-foo-int8-band0.arg")
//    println(m.asciiDraw)
//    
//    assert(loadRaster("/tmp/multiband-foo1/multiband-foo-int8-band1.arg").toArray === array2)
//    
//    assert(loadRaster("/tmp/multiband-foo2/multiband-foo-int8-band1.arg").toArray === array2)
//    
//    assert(loadRaster("/tmp/multiband-foo1/multiband-foo-int8-band2.arg").toArray === array3)
//    
//    assert(loadRaster("/tmp/multiband-foo2/multiband-foo-int8-band2.arg").toArray === array3)
//    
//  }
//
//  test("check int16") {
//    assert(loadRaster("/tmp/multiband-foo1/multiband-foo-int16-band0.arg").toArray === array1)
//    assert(loadRaster("/tmp/multiband-foo2/multiband-foo-int16-band0.arg").toArray === array1)
//  }

  /**
   * to run this check there should be /tmp/multiband-foo1/multiband-foo-float32-band0.arg file
   */
  
//  test("check float32") {
//    val d = loadRaster("/tmp/multiband-foo1/multiband-foo-float32-band0.arg").toArrayRaster.data
//    assert(isNoData(d.applyDouble(0)))
//    assert(d.applyDouble(1) === -1.0)
//    assert(d.applyDouble(2) === 2.0)
//  }
//  
  test("check MultiBandReader") {
    MultiBandArgWriter(TypeInt, "/tmp/multiband-foo/multiband-foo").write(multibandRaster, "multiband-foo")
    //printf(MultiBandArgReader.getFileList("/tmp/multiband-foo/").toString)
    assert(MultiBandArgReader.read("/tmp/multiband-foo/", TypeInt, re1) === multibandRaster)
    assert(MultiBandArgReader.read("/tmp/multiband-foo/", TypeInt, re1,re1) === multibandRaster)  
    
    MultiBandArgWriter2(TypeInt, "/tmp/multiband-foo3/multiband-foo3").write(multibandRaster, "multiband-foo3")
    printf(MultiBandArgReader.getFileList("/tmp/multiband-foo3/").toString)
    //assert(MultiBandArgReader.read("/tmp/multiband-foo3/", TypeInt, re1) === multibandRaster)
    //assert(MultiBandArgReader.read("/tmp/multiband-foo/", TypeInt, re1,re1) === multibandRaster) 
    
//    assert(MultiBandArgReader.read("/tmp/multiband-foo3/", TypeInt, re1) === MultiBandArgReader.read("/tmp/multiband-foo/", TypeInt, re1))
  }
  
  
                
}