package geotrellis.raster.io.arg

import java.io.{ BufferedOutputStream, DataOutputStream, FileOutputStream }

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
 * MultiBandArgWriter will write a MultiBandRaster to disk in ARG format.
 *
 * Each instance of MultiBandArgWriter is provided a data type (e.g. int or float) to
 * use for output files.
 */

case class MultibandArgWriter(typ: CellType) {
  def width = typ.bits / 8
  def cellType = typ.name
  def dataType = "arg"

  /**
   * Write a MultiBandRaster in ARG format to the specified path.
   *
   * The outputFilePath argument should be the full file path (including file name and extension)
   * to which the MultiBandRaster file should be written.  For example, "/var/data/geotrellis/myraster.arg".
   *
   * The metadataName argument is a logical name for the MultiBandRaster that will be included in the MultiBandRaster's
   * metadata.  It does not have to match the output filename.  This filename is used by the catalog
   * when a MultiBandRaster is loaded by name as opposed to filepath.
   *
   * @param outputFilePath: File path to which to write out arg files (can include .arg extension)
   * @param multiBandTile: MultiBandRaster to write to disk
   * @param metadataName: Name to be included in json metadata as 'layer', used in catalog
   */

  def write(outputFilePath: String, multiBandTile: MultiBandTile, extent: Extent, metadataName: String) {
    val path = outputFilePath
    val base: String = if (path.endsWith(".arg") || path.endsWith(".json") || path.endsWith(".")) {
      path.substring(0, path.lastIndexOf("."))
    } else {
      path
    }

    writeMetadataJSON(base, metadataName, multiBandTile.bands, RasterExtent(extent, multiBandTile.cols, multiBandTile.rows))

    cfor(0)(_<multiBandTile.bands,_+1){ band=>
      writeData(base + "-band" + band + ".arg", multiBandTile.getBand(band))
    }
  }

  private def writeMetadataJSON(path: String, name: String, noOfBands: Int, re: RasterExtent) {
    val metadata = s"""{
        |  "layer": "$name",
        |  "datatype": "$cellType",
        |  "type": "$dataType",
        |  "bands": "$noOfBands",
        |  "path": "$path",
        |  "xmin": ${re.extent.xmin},
        |  "xmax": ${re.extent.xmax},
        |  "ymin": ${re.extent.ymin},
        |  "ymax": ${re.extent.ymax},
        |  "cols": ${re.cols},
        |  "rows": ${re.rows},
        |  "cellwidth": ${re.cellwidth},
        |  "cellheight": ${re.cellheight},
        |  "epsg": 3785,
        |  "yskew": 0.0,
        |  "xskew": 0.0
        |}""".stripMargin

    val bos = new BufferedOutputStream(new FileOutputStream(path + ".json"))
    bos.write(metadata.getBytes)
    bos.close
  }

  def writeData(path: String, raster: Tile) {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
    CellWriter.byType(typ).writeCells(raster, dos)
    dos.close()
  }

}