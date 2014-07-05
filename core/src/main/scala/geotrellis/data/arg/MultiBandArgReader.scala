/**
 * @author kelum
 *
 */

package geotrellis.data.arg

import geotrellis._
import geotrellis.data.arg._

import java.io._

/**
 * path should be the path to MultiBandRaster directory
 * example if MultiBandRaster is multiband-foo and it is in /tmp/
 * then path shoul be "/tmp/multiband-foo/"
 *
 */


object MultiBandArgReader {

  def read(path: String, typ: RasterType, rasterExtent: RasterExtent): Array[Raster] =
    read(path) { f => ArgReader.read(f, typ, rasterExtent) }

  def read(path: String, typ: RasterType, rasterExtent: RasterExtent, targetExtent: RasterExtent): Array[Raster] =
    read(path) { f => ArgReader.read(f, typ, rasterExtent, targetExtent) }

  private def read(path: String)(f: String => Raster): Array[Raster] =
    getFileList(path).sortBy(f => f.substring(f.lastIndexOf("band")+4, f.indexOf(".arg")).toInt)
      .map(f)
      .toArray
      
  def getFileList(path: String): List[String] = {
    val pathList: List[String] = new java.io.File(path).listFiles.filter(_.getName.endsWith(".arg")).toList map {a => path+a.getName}    
    
    if(pathList.isEmpty) throw new FileNotFoundException("Raster.isEmpty")
    else pathList
  }
  
}