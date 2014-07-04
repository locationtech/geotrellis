/**
 * @author kelum
 *
 */

package geotrellis.data.arg

import geotrellis._
import geotrellis.data.arg.ArgWriter._

/**
 *   before providing OutPutFilePath new folder should be created which should be named as raster name.
 *   then path should be path to that directory and raster name.
 *   
 *   example : if raster name is multiband-foo then path should be ".../multiband-foo/multiband-foo
 *
 */

case class MultiBandArgWriter(val rType: RasterType, outputFilePath: String) {

  val path = outputFilePath
  type MultiBandRaster = Array[Raster]
  
  def write(bandRaster: MultiBandRaster, metadataName: String){
    for(index <- 0 until bandRaster.length) yield ArgWriter(rType).write(outputFilePath+"-band"+index, bandRaster(index), metadataName)
  }
  
}