package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.process._

/**
 * Task that converts geotiff rasters into arg32 rasters.
 *
 * See trellis.run.Task for more information on Tasks.
 */
@Parameters(commandNames = Array("geotiff_convert"), commandDescription ="Convert a geotiff raster into ARG format.") 
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

  @Parameter(
    names = Array("--type", "-t"),
    description = "data type of output raster [bit|byte|int|float|double] (default depends on input)",
    required=false
  ) 
  var datatype:String = ""

  val taskName = "geotiff_convert"  

  def execute = { 
    GeotiffImportTask.execute(this.inPath, this.outPath, this.name, this.datatype)  
  } 
} 

object GeotiffImportTask {


  /**
    * Maps from string representing data type to RasterType.
    */
  def stringToRasterType(s:String):RasterType = {
    s match {
      case "bit"      => TypeBit
      case "byte"     => TypeByte
      case "short"    => TypeShort
      case "int"      => TypeInt
      case "float"    => TypeFloat
      case "double"   => TypeDouble
      case _          => throw new Exception(s"Unknown raster datatype $s: see --help for valid options")
    } 
  }

  def execute(inpath:String, outpath:String, name:String, datatype:String):Unit = {    
    if(! new java.io.File(inpath).exists) {
      println(s"File $inpath does not exist")
      return
    }

    val server = Server("script", Catalog.empty("script"))
    try {
      println("Loading file: " + inpath)
      //val raster = server.run(io.LoadFile(inpath))
      val raster = GeoTiffReader.readPath(inpath,None,None)
     
      val version = "1.0"
  
      val rastertype = if (datatype == "") raster.data.getType else stringToRasterType(datatype)
    
      println("Converting geotiff: " + inpath)
      val writer = ArgWriter(rastertype).write(outpath, raster, name)

      println("ARG file generated: " + outpath )
    } finally { server.shutdown }
  }
}
