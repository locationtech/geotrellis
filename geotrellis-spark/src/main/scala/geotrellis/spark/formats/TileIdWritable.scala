package geotrellis.spark.formats

import org.apache.hadoop.io.LongWritable
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class TileIdWritable extends LongWritable with Serializable {
  override def equals(that: Any) : Boolean = {
    //println(s"called equals on ${get}, isInstanceOf = ${that.isInstanceOf[TileIdWritable]} and [${get},${that.asInstanceOf[TileIdWritable].get}")
    that.isInstanceOf[TileIdWritable] &&
    (this.get == that.asInstanceOf[TileIdWritable].get)
  }
  
  private def writeObject(out: ObjectOutputStream) {
    //println("called TileIdWritable writeObject " + get)
    out.defaultWriteObject()
    out.writeLong(get)
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    set(in.readLong)
    //println("called TileIdWritable readObject " + get)
  }
} 

object TileIdWritable {
  def apply(value: Long): TileIdWritable = {
    val tw = new TileIdWritable
    tw.set(value)
    tw
  }
  def apply(tw: TileIdWritable): TileIdWritable = {
    TileIdWritable(tw.get())
  }  
}
