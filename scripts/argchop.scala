#!/bin/sh
export JAVA_OPTS="-XX:MaxPermSize=1024m -Xms512m -Xmx4096m"
exec scala -cp $HOME/trellis/target/scala_2.8.0/classes -Xscript Script "$0" "$@"
!#

import Console.printf

import scala.math.{floor, ceil, round}

import trellis.data.{Reader, Writer};
import trellis.data.arg.{ArgReader, ArgWriter};
import trellis.data.arg32.{Arg32Reader, Arg32Writer};
import trellis.core.geoattr.{GeoAttrs};
import trellis.process.{Server};
import trellis.operation.int.{LoadFile, WrapGeoAttrs};

import java.lang.Class
import java.lang.reflect.Constructor

def error(msg:String) {
  if (msg.length > 0) {
    Console.printf("ERROR: %s\n\n", msg)
  }
  Console.printf("usage: argchop.scala IN-BASE OUT-BASE X1 Y1 X2 Y2 [W H]\n")
  exit(1)
}

def main(args:Array[String]) {
  val len = args.length
  if (len != 6 && len != 8) error("wrong number of arguments (%d)".format(len))
  
  val in  = args(0)
  val out = args(1)
  val x1  = args(2).toDouble
  val y1  = args(3).toDouble
  val x2  = args(4).toDouble
  val y2  = args(5).toDouble

  val server = Server("chop")
  server.start

  val rdr = if (in.endsWith(".arg32")) {
    new Arg32Reader(in.substring(0, in.length - 6), server)
  } else if (in.endsWith(".arg")) {
    new ArgReader(in.substring(0, in.length - 4), server)
  } else {
    throw new Exception("urk")
  }

  rdr.readMetadata

  val rows = if (len == 8) {
    args(6).toInt
  } else {
    ceil((y2 - y1) / rdr.cellheight).toInt
  }

  val cols = if (len == 8) {
    args(7).toInt
  } else {
    ceil((x2 - x1) / rdr.cellwidth).toInt
  }

  printf(" !! got cols=%d x rows=%d\n", cols, rows)

  val geo = new GeoAttrs(x1, y1, x2, y2, rdr.cellwidth, rdr.cellheight,
                         cols, rows, rdr.srid)
  printf(" -- rdr saw cw=%f (%s) ch=%f (%s)\n", rdr.cellwidth, rdr.cellwidth,
         rdr.cellheight, rdr.cellheight)
  printf(" -- geoattrs are %s\n", geo)

  val raster = LoadFile(in, WrapGeoAttrs(geo)).run(server)
  printf(" -- r.geoattrs are %s\n", raster.geoattrs)

  val cls = if (out.endsWith("arg32")) {
    classOf[Arg32Writer]
  } else {
    classOf[ArgWriter]
  }
  val c = cls.getDeclaredConstructors()(0)
  val w:Writer = c.newInstance(raster, out, "VERSION", "ID", "NAME",
                               "DESCRIPTION", raster.geoattrs.projection.toString,
                               "CREATE-DATE", "UPDATE-DATE").asInstanceOf[Writer]
  w.writeRaster
}

main(args)
