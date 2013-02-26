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
@Parameters(commandNames = Array("import-tile"), commandDescription ="Imports raster data as a tiled ARG using GDAL.")
class ImportTileTask extends Task { 
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

  @Parameter(
    names = Array("--cols", "-c"),
    description = "Pixel columns per tile (width of each tile in pixels)",
   required=true
  )
  var cols:Int = _

  @Parameter(
    names = Array("--rows", "-r"),
    description = "Pixel rows per tile (height of each tile in pixels)",
    required=true
  )
  var rows:Int = _

  val taskName = "import-tile"
 
  def execute = { 
    ImportTileTask.execute(inPath, outDir, name, cols, rows)  
  } 
} 

object ImportTileTask {
  def execute(inPath:String, outDir:String, name:String, cols:Int, rows:Int) {  
    println("Writing tiled raster data using GDAL...")
    Tiler.writeTilesWithGdal(inPath, name, outDir, cols, rows)
  }
}



