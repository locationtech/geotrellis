package geotrellis.run

import java.util.Calendar

import geotrellis.data.GeoTiffReader
import geotrellis.data.Arg32Writer
import geotrellis.process.{Server}
import geotrellis.operation.LoadFile
import geotrellis.process._

object GeotiffImporter {
  def error(msg:String) {
    if (msg.length > 0) {
      Console.printf("ERROR: %s\n\n", msg)
    }
    Console.printf("usage: geotif-to-arg32.scala INPATH OUTPATH\n")
    sys.exit(1)
  }

  def main(args:Array[String]) {
    if (args.length != 2) error("wrong number of arguments (%d)".format(args.length));

    val inpath  = args(0)
    val outpath = args(1)
    
    val server = Server("script", Catalog.empty("script"))

    println("Loading file: " + inpath)
    val raster = server.run(LoadFile(inpath))
    server.shutdown()
    //val reader = new GeoTiffReader(inpath, server)
    //val raster = reader.loadRaster
    
    val version = "1.0"
    
    val name = "Generated from " + inpath
    
    
    println("Converting file to ARG32 format: " + inpath)
    val writer = Arg32Writer.write(outpath, raster, name)

    println("ARG file generated: " + outpath )

  }
}

// vim: set ts=4 sw=4 et:
