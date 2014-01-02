package geotrellis.spark.formats

import org.apache.hadoop.io.LongWritable

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class TileIdWritable extends LongWritable with Serializable {
  override def equals(that: Any): Boolean =
    that match {
      case other: TileIdWritable => other.get == this.get
      case _ => false
    }

  override def hashCode = get.hashCode
  
  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeLong(get)
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    set(in.readLong)
  }
}

object TileIdWritable {
  def apply(value: Long): TileIdWritable = {
    val tw = new TileIdWritable
    tw.set(value)
    tw
  }
  def apply(tw: TileIdWritable): TileIdWritable = {
    TileIdWritable(tw.get)
  }
}
