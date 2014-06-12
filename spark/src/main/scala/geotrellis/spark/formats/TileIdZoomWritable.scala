package geotrellis.spark.formats

import java.io.DataOutput
import java.io.DataInput

class TileIdZoomWritable extends TileIdWritable with Serializable {

  /*
   * This is vars only because of readFields needing to reassign it
   * this is not in the class definition because I need a default
   * constructor (new TileIdZoomWritable()) and I'm not sure how to 
   * get a default constructor 
   */
  private var _zoom: Int = _

  def zoom = _zoom
  def setZoom(z: Int): TileIdZoomWritable = {
    _zoom = z
    this
  }

  override def equals(that: Any): Boolean =
    that match {
      case other: TileIdZoomWritable => 
        _zoom == other.zoom && super.equals(that)
      case _ => false
    }

  override def write(out: DataOutput) {
    super.write(out)
    out.writeInt(zoom)
  }

  override def readFields(in: DataInput) {
    set(in.readLong())
    _zoom = in.readInt()
  }
}

object TileIdZoomWritable {
  def apply(tileId: Long, zoom: Int): TileIdZoomWritable = {
    val tzw = new TileIdZoomWritable
    tzw.setZoom(zoom).set(tileId)
    tzw
  }
}
