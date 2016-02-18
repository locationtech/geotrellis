package geotrellis.raster

import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

trait TestFiles {

  def loadTestArg(name: String) = {
    ArgReader.read(s"raster-test/data/$name.json")
  }
}
