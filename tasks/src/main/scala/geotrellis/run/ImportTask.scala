package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.process._
import geotrellis.raster._

/**
 * Task that import raster data into a tiled ARG format.
 *
 * See trellis.run.Task for more information on Tasks.
 */
@Parameters(commandNames = Array("import"), commandDescription ="Imports raster data as a tiled raster into the geotrellis system.")
class ImportTask extends Task { 
  @Parameter( 
    names = Array("--input", "-i"),  
    description = "Path of raster file",  
    required=true) 
  var inPath:String = _  
 
  @Parameter( 
    names = Array("--dir", "-d"),   
    description = "Path of output directory",
    required=true) 
  var outDir:String = _ 
 
  @Parameter( 
    names = Array("--name", "-n"),  
    description = "Name of output raster",
    required=true 
  ) 
  var name:String = _ 

  val taskName = "import"
 
  def execute = {
    ImportTask.execute(inPath, outDir, name)
  } 
} 

object ImportTask {
  def execute(inPath:String, outDir:String, name:String) {  
    if(!(new java.io.File(inPath).exists())) {
      println("File %s does not exist.".format(inPath))
      return
    }
    println("Writing raster data using GDAL...")
    Importer.importWithGdal(inPath, name, outDir)
  }
}



