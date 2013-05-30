package geotrellis.data

import geotrellis._

trait IntReadState extends ReadState {
  // must override
  def getNoDataValue:Int

  protected[this] override def translate(data:MutableRasterData) {
    var i = 0
    val len = data.length
    val nd = getNoDataValue
    while (i < len) {
      if (data(i) == nd) data(i) = NODATA
      i += 1
    }
  }
}
