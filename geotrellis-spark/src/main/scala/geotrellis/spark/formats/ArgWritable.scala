package geotrellis.spark.formats

import geotrellis._
import geotrellis.RasterType
import geotrellis.raster.RasterData
import org.apache.hadoop.io.BytesWritable
import org.apache.spark.Logging

class ArgWritable(bytes: Array[Byte]) extends BytesWritable(bytes) with Logging {
  /* This constructor is used by hadoop, (e.g., hadoop fs -text) and  
   * Spark (HadoopRDD.compute does reader.createValue()). Geotrellis 
   * developers are encouraged to use one of the "apply" methods instead
   */
  def this() = this(Array[Byte]())

  def toRasterData(awType: RasterType, cols: Int, rows: Int) = {
    /* 
     * The slice is done in cases where the backing byte array in BytesWritable 
     * is larger than expected, so a simple getBytes would get the larger array
     * and RasterData would be thrown off. See BytesWritable.setSize, which calls
     * setCapacity with 1.5 * size if the array needs to be grown
     * 
     * BitArrayRasterData is a bit special since every row is a byte, cols = 8  
     *
     */
    val bytes = awType match {
      case TypeBit => rows
      case _ => cols * rows * awType.bytes
    }
    RasterData.fromArrayByte(getBytes.slice(0, bytes), awType, cols, rows)
  }
}

object ArgWritable {
  def apply(len: Int, fillValue: Byte) = new ArgWritable(Array.ofDim[Byte](len).fill(fillValue))
  def apply(bytes: Array[Byte]) = new ArgWritable(bytes)
  def apply(aw: ArgWritable) = new ArgWritable(aw.getBytes)

  def fromRasterData(data: RasterData) = ArgWritable(data.toArrayByte)
}