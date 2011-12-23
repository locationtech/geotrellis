#!/bin/sh
export JAVA_OPTS="-XX:MaxPermSize=1024m -Xms512m -Xmx4096m"
exec scala -classpath $HOME/trellis/target/scala_2.8.0/classes -Xscript Script "$0" "$@"
!#

// NOTE: you can't actually run this thing directly due to of classpath issues.
// sorry!

import java.util.Calendar

import trellis.data.geotiff.GeoTiffReader
import trellis.data.arg32.Arg32Writer
import trellis.process.{Server}
import trellis.operation.LoadFile

def error(msg:String) {
  if (msg.length > 0) {
    Console.printf("ERROR: %s\n\n", msg)
  }
  Console.printf("usage: geotif-to-arg32.scala INPATH OUTPATH\n")
  exit(1)
}

def main(args:Array[String]) {
  if (args.length != 2) error("wrong number of arguments (%d)".format(args.length));

  val inpath  = args(0)
  val outpath = args(1)
  
  val server = Server("geotiff-to-arg32")
  server.start

  val raster = server.run(LoadFile(inpath))
  //val reader = new GeoTiffReader(inpath, server)
  //val raster = reader.loadRaster
  
  val version = "1.0"
  
  val id   = "id"
  val name = "My new arg32 raster"
  val desc = "created by geotiff-to-arg32"
  //val srid = "???"
  
  val date = Calendar.getInstance.getTime.toString
  
  val writer = new Arg32Writer(raster, outpath, version, id, name, desc,
                               raster.geoattrs.extent.srs.toString, date, date)
  writer.writeRaster
  println("done")

}

main(args)
