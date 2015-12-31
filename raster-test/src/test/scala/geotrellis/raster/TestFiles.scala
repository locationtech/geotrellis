package geotrellis.raster

import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

trait TestFiles {

  def loadTestArg(name: String) = {
    ArgReader.read(s"raster-test/data/$name.json")
  }

  def loadGeoTiff(name: String): Raster = {
    GeoTiffReader.readSingleBand(s"data/nn/$name.tiff").raster
  }
}
