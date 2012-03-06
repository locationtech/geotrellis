package trellis.run

import java.util.Calendar

import trellis.data.GeoTiffReader
import trellis.data.Arg32Writer
import trellis.process.{Server}
import trellis.operation.LoadFile
import trellis.process._
import trellis._
import trellis.operation._
import trellis.raster._

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


