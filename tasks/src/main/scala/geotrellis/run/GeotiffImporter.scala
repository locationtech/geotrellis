package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.operation._
import geotrellis.process._


/**
 * Task that converts geotiff rasters into arg32 rasters.
 *
 * See trellis.run.Task for more information on Tasks.
 */
@Parameters(commandNames = Array("geotiff_convert"), commandDescription ="Convert a geotiff raster into an arg32 raster.") 
class GeotiffImportTask extends Task { 
  @Parameter( 
    names = Array("--input", "-i"),  
    description = "Path of geotiff file to import",  
    required=true) 
  var inPath:String = _  
 
  @Parameter( 
    names = Array("--output", "-o"),   
    description = "Path of arg32 file to export",  
    required=true) 
  var outPath:String = _ 
 
  @Parameter( 
    names = Array("--name", "-n"),  
    description = "Name of arg32 to include in metadata", 
    required=true 
  ) 
  var name:String = _ 
 
  val taskName = "geotiff_convert"  
 
  def execute = { 
    GeotiffImporter.execute(this.inPath, this.outPath, this.name)  
  } 
} 

object GeotiffImporter {
  def execute(inpath:String, outpath:String, name:String) {    
    val server = Server("script", Catalog.empty("script"))

    println("Loading file: " + inpath)
    val raster = server.run(LoadFile(inpath))
    server.shutdown()
    
    val version = "1.0"
    
    println("Converting geotiff: " + inpath)
    val writer = ArgWriter(TypeInt).write(outpath, raster, name)

    println("ARG file generated: " + outpath )

  }
}
