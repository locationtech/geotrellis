package geotrellis.data

import geotrellis.util.Filesystem
import geotrellis.util.Filesystem

object Importer {
  def importWithGdal(inPath: String, name: String, outDir:String) {
    val rasterInfo = Gdal.info(inPath)
    rasterInfo.rasterType match {
      case Some(rasterType) => {
        val dir = new java.io.File(outDir)
        if (!dir.exists()) dir.mkdir()
        val outPath = Filesystem.join(outDir, name + ".arg")
        Gdal.translate(inPath, outPath, rasterType)
      }
      case None => println("ERROR: This file is of a raster type that is not compatable with geotrellis.")
    }
  }
}
