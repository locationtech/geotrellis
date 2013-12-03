package geotrellis.spark.formats

import org.apache.hadoop.io.LongWritable

class TileIdWritable extends LongWritable 

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
