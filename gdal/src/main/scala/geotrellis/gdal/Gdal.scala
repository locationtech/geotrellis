package geotrellis.gdal

import org.gdal.gdal.gdal
import org.gdal.gdal.Dataset
import org.gdal.gdalconst.gdalconstConstants

class GdalException(code: Int, msg: String) 
    extends RuntimeException(s"GDAL ERROR $code: $msg")

object GdalException {
  def lastError(): GdalException =
    new GdalException(gdal.GetLastErrorNo,
                      gdal.GetLastErrorMsg)
}

object Gdal {
  gdal.AllRegister()

  def open(path: String): RasterDataSet = {
    val ds = gdal.Open(path, gdalconstConstants.GA_ReadOnly)
    if(ds == null) {
      throw GdalException.lastError()
    }
    new RasterDataSet(ds)
  }
}
