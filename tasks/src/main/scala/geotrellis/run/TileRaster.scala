package geotrellis.run

import java.util.Calendar

import geotrellis.data.GeoTiffReader
import geotrellis.data.Arg32Writer
import geotrellis.process.{Server}
import geotrellis.operation.LoadFile
import geotrellis.process._
import geotrellis._
import geotrellis.operation._
import geotrellis.raster._

object TileRaster {
  def error(msg:String) {
    if (msg.length > 0) {
      Console.printf("ERROR: %s\n\n", msg)
    }
    Console.printf("usage: geotif-to-arg32.scala INPATH NAME OUTPATH\n")
    sys.exit(1)
  }

  def main(args:Array[String]) {
    if (args.length != 3) error("wrong number of arguments (%d)".format(args.length));

    val inpath  = args(0)
    val name    = args(1)
    val outpath  = args(2)
   
    execute(inpath, name, outpath)
  } 

  def execute(inpath:String, name:String, outpath:String) {
    val server = Server("script", Catalog.empty("script"))

    println("Loading file: " + inpath)
    val raster = server.run(LoadFile(inpath))
    server.shutdown()
    //val reader = new GeoTiffReader(inpath, server)
    //val raster = reader.loadRaster
   

    val trd = Tiler.createTileRasterData(raster, 256)
    Tiler.writeTiles(trd, name, outpath)
 
    println("Creating tiled raster: " + inpath)

  }
}

// vim: set ts=4 sw=4 et:


