package geotrellis.spark.formats

import java.io.DataOutput
import java.io.DataInput

class MultiLevelTileIdWritable extends TileIdWritable with Serializable {

  /*
   * These are vars only because of readFields needing to reassign them
   * these are not in the class definition because I need a default
   * constructor (new MultiLevelTileIdWritable()) and I'm not sure how to 
   * get a default constructor 
   */
  private var _zoom: Int = _
  private var _parentTileId: Long = _

  def zoom = _zoom
  def setZoom(z: Int): MultiLevelTileIdWritable = {
    _zoom = z
    this
  }

  def parentTileId = _parentTileId
  def setParentTileId(p: Long): MultiLevelTileIdWritable = {
    _parentTileId = p
    this
  }

  override def equals(that: Any): Boolean =
    that match {
      case other: MultiLevelTileIdWritable => _zoom == other.zoom && _parentTileId == other._parentTileId && super.equals(that)
      case _                               => false
    }

  override def write(out: DataOutput) {
    super.write(out)
    out.writeInt(zoom)
    out.writeLong(_parentTileId)
  }

  override def readFields(in: DataInput) {
    set(in.readLong())
    _zoom = in.readInt()
    _parentTileId = in.readLong()
  }
}
object MultiLevelTileIdWritable {
  def apply(tileId: Long, zoom: Int, parentTileId: Long): MultiLevelTileIdWritable = {
    val mltw = new MultiLevelTileIdWritable
    mltw.setParentTileId(parentTileId)
      .setZoom(zoom)
      .set(tileId)
    mltw
  }
}