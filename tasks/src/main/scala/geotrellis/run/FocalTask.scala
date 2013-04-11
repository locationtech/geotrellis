package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.raster.op.focal._
import geotrellis.io

/**
 * Task that diffs two rasters.
 *
 * The diff output provides information about how the two rasters differ.
 */
@Parameters(commandNames = Array("focal"), commandDescription ="Runs focal operations on a raster.")
class FocalTask extends Task { 
  @Parameter( 
    names = Array("--source", "-s"),  
    description = "Path of source ARG raster (to be diffed against)",  
    required=true) 
  var sourcePath:String = _  
 
  @Parameter( 
    names = Array("--target", "-t"),   
    description = "Path of target ARG raster (to diff against source)",
    required=true) 
  var targetPath:String = _ 

  @Parameter( 
    names = Array("--operation", "-o"),   
    description = "Operation to run - currently one of [aspect,slope,hillshade]",
    required=true) 
  var operation:String = _ 
 
  val taskName = "focal"
 
  def execute = {
    FocalTask.execute(sourcePath, targetPath, operation)
  } 
} 

object FocalTask {
  def doesFileExist(p:String) = {
    if(!(new java.io.File(p).exists())) {
      println("File %s does not exist.".format(p))
      false
    }
    true
  }

  def execute(sourcePath:String, targetPath:String, operation:String):Unit = {  
    if(!doesFileExist(sourcePath)) { return }
    if(!targetPath.endsWith(".arg")) { 
      println("Target path must end in .arg")
      return
    }

    val localServer = Server.empty("task")
    
    try {
      val op = operation.toLowerCase match {
        case "aspect" =>
          Aspect(io.LoadFile(sourcePath))
        case "slope" =>
          Slope(io.LoadFile(sourcePath),1.0)
        case "hillshade" =>
          Hillshade(io.LoadFile(sourcePath))
        case _ =>
          null
      }

      if(op == null) {
        println("Unknown focal operation ${operation}")
        return
      }

      val r = localServer.run(op)

      val name = new java.io.File(targetPath).getName.replace(".arg","")
      ArgWriter(r.data.getType).write(targetPath, r, name)
      println(s"Wrote to ${targetPath}")
    } finally {
      localServer.shutdown()
    }
  }
}
