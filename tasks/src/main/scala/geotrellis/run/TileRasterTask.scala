package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.process._
import geotrellis.raster._

/**
 * Task that writes a tile set to disk from a standard raster.
 *
 * See trellis.run.Task for more information on Tasks.
 */
@Parameters(commandNames = Array("tile-raster"), commandDescription ="Generate a tiled raster from an raster in ARG format.")
class TileRasterTask extends Task { 
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

  val taskName = "tile-raster"
 
  def execute = { 
    TileRasterTask.execute(inPath, outDir, name, cols, rows)  
  } 
} 

object TileRasterTask {
  def execute(inPath:String, outDir:String, name:String, cols:Int, rows:Int) {    
    val server = Server("script", Catalog.empty("script"))

    println("Loading raster at path: " + inPath)
    val raster = server.run(io.LoadFile(inPath))

    println("Generating tiles in directory: " + outDir)

    val trd = Tiler.createTiledRasterData(raster, cols, rows)
    Tiler.writeTiles(trd, raster.rasterExtent, name, outDir)
 
    server.shutdown()

    println("Tiles generated.") 
  }
}
