/**
 * @author kelum
 *
 */

package geotrellis.data.arg

import java.io.{ BufferedOutputStream, DataOutputStream, FileOutputStream }

import geotrellis._
import geotrellis.data._
import geotrellis.util._
import geotrellis.process._

/**
 *   before providing OutPutFilePath new folder should be created which should be named as raster name.
 *   then path should be path to that directory and raster name.
 *   
 *   example : if raster name is multiband-foo then path should be ".../multiband-foo/multiband-foo
 *
 */


case class MultiBandArgWriter2(val rType: RasterType, outputFilePath: String) {

  val path = outputFilePath
  type MultiBandRaster = Array[Raster]
  def dataType = "arg"

  def write(bandRaster: MultiBandRaster, metadataName: String) {
    val base: String = if (path.endsWith(".arg") || path.endsWith(".json") || path.endsWith(".")) {
      path.substring(0, path.lastIndexOf("."))
    } else {
      path
    }
    writeMetadataJSON(base + ".json", metadataName, bandRaster(0).rasterExtent)
    for (index <- 0 until bandRaster.length) yield writeData(base + "-band" + index + ".arg", bandRaster(index))
  }

  private def writeBytes(data: RasterData, cols: Int, rows: Int, dos: DataOutputStream) {
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        dos.writeByte(data.get(col, row))
        col += 1
      }
      row += 1
    }
  }

  private def writeData(path: String, raster: Raster) {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
    CellWriter.byType(rType).writeCells(raster, dos)
    dos.close()
  }

  private def writeMetadataJSON(path: String, name: String, re: RasterExtent) {
    val metadata = """{
      "layer": "%s",
      "datatype": "%s", 
      "type": "%s",
      "xmin": %f,
      "xmax": %f,
      "ymin": %f,
      "ymax": %f,
      "cols": %d,
      "rows": %d,
      "cellwidth": %f,
      "cellheight": %f,
      "epsg": 3785,
      "yskew": 0.0,
      "xskew": 0.0
      }""".format(name, rType, dataType, re.extent.xmin, re.extent.xmax, re.extent.ymin,
      re.extent.ymax, re.cols, re.rows, re.cellwidth, re.cellheight)

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}