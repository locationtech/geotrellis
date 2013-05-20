package geotrellis.data

import geotrellis._

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

trait Writer {
  def write(path:String, raster:Raster, name:String):Unit

  def rasterType: String
  def dataType:String

  def writeMetadataJSON(path:String, name:String, re:RasterExtent) {
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
}""".format(name, rasterType, dataType, re.extent.xmin, re.extent.xmax, re.extent.ymin,
             re.extent.ymax, re.cols, re.rows, re.cellwidth, re.cellheight)

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}
