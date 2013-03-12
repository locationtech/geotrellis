package geotrellis.run

import com.beust.jcommander._

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.process._

/**
 * Task that converts a directory of geotiff rasters into arg32 rasters.
 *
 * See trellis.run.Task for more information on Tasks.
 */
@Parameters(commandNames = Array("geotiff-batch-import"), commandDescription ="Convert geotiff's in a directory into ARG format.") 
class GeotiffBatchImportTask extends Task { 
  @Parameter( 
    names = Array("--input", "-i"),  
    description = "Directory containing GeoTiff files (.tif)",  
    required=true) 
  var inPath:String = _  
 
  @Parameter( 
    names = Array("--output", "-o"),   
    description = "Directory in which to put the ARG files.",  
    required=true) 
  var outPath:String = _ 
 
  @Parameter( 
    names = Array("--pre", "-p"),  
    description = "Prefix to append to name of ARG to include in metadata. (Name will be .tif file name)",
    required = false
  ) 
  var prefix:String = ""

  @Parameter(
    names = Array("--type", "-t"),
    description = "data type of output raster [bit|byte|int|float|double] (default depends on input)",
    required=false
  ) 
  var datatype:String = _

  val taskName = "geotiff-batch-import"  

  def execute = { 
    GeotiffBatchImportTask.execute(this.inPath, this.outPath, this.prefix, this.datatype)
  } 
} 

object GeotiffBatchImportTask {
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

  def execute(inpath:String, outpath:String, prefix:String, datatype:String):Unit = {    
    val inputDir = new java.io.File(inpath)
    if(!inputDir.isDirectory) {
      println(s"Directory $inpath does not exist")
      return
    }

    val outputDir = new java.io.File(outpath)
    if(!outputDir.isDirectory) {
      if(!outputDir.mkdirs()) {
        println(s"Could not create directory $outpath.")
      }
    }
    
    val tifs = inputDir.listFiles.filter( f => f.toString.endsWith(".tif") )
    if(tifs.length < 1) {
      println(s"No .tif files in $inpath")
      return
    }


    val server = Server("script", Catalog.empty("script"))

    try {
      for(tif <- tifs) {
        val name = prefix + tif.toPath.getFileName.toString.replace(".tif","")
        val argPath = new java.io.File(outputDir.toString, name + ".arg").toString
        val tifPath = tif.toString
        
        println("Loading file: " + tifPath)

        val raster = GeoTiffReader.readPath(tifPath,None,None)
        
        val rastertype = if (datatype == "") raster.data.getType else stringToRasterType(datatype)
        
        println("Converting geotiff: " + tifPath)
        val writer = ArgWriter(rastertype).write(argPath, raster, name)

        println("ARG file generated: " + argPath )
      }
    } finally { server.shutdown }
  }
}
