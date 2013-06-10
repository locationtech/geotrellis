package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.io

/**
 * Gives information about an ARG raster.
 */
@Parameters(commandNames = Array("info"), commandDescription ="Provides information about an ARG raster.")
class InfoTask extends Task { 
  @Parameter( 
    names = Array("--source", "-s"),  
    description = "Path of source ARG raster.",  
    required=true) 
  var sourcePath:String = _  
 
  val taskName = "info"
 
  def execute = {
    InfoTask.execute(sourcePath)
  } 
} 

object InfoTask {
  def doesFileExist(p:String) = {
    if(!(new java.io.File(p).exists())) {
      println("File %s does not exist.".format(p))
      false
    }
    true
  }

  def execute(sourcePath:String):Unit = {  
    if(!doesFileExist(sourcePath)) { return }

    val localServer = Server.empty("task")
    
    try {
      val info = localServer.run(io.LoadRasterLayerInfoFromPath(sourcePath))
      val raster = localServer.run(io.LoadFile(sourcePath))

      println(s"ARG at $sourcePath")
      println("------------------------------------------")
      println(s"Data Type: ${info.rasterType}")
      println(s"Raster Exent: ${info.rasterExtent}")

      if(info.rasterType.isDouble) {
        val (min,max) = raster.findMinMaxDouble
        println(s"Min $min  Max $max")
        var sum = 0.0
        var count = 0
        raster.foreachDouble { z => if(!java.lang.Double.isNaN(z)) { sum += z; count += 1; } }
        println(s"Mean: ${sum/count}")
      } else {
        val (min,max) = raster.findMinMax
        println(s"Min $min  Max $max")
        var sum = 0
        var count = 0
        raster.foreach { z => if(z != NODATA) { sum += z; count += 1; 1 } }
        println(s"Mean: ${sum/count}")
      }
    } finally {
      localServer.shutdown()
    }
  }
}
