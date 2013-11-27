package geotrellis.spark.formats

import java.nio.ByteBuffer

import org.apache.hadoop.io.BytesWritable

class ArgWritable extends BytesWritable

object ArgWritable {
  
  // constant sizes of primitive types
  import java.lang.{Float=>JavaFloat, Double=>JavaDouble, Long=>JavaLong, Integer=>JavaInt, Short=>JavaShort, Byte=>JavaByte}  
  val BytesBytes = 1
  val FloatBytes = JavaFloat.SIZE / JavaByte.SIZE
  val DoubleBytes = JavaDouble.SIZE / JavaByte.SIZE
  val LongBytes = JavaLong.SIZE / JavaByte.SIZE
  val IntBytes = JavaInt.SIZE / JavaByte.SIZE
  val ShortBytes = JavaShort.SIZE / JavaByte.SIZE

  def apply(bytes: Array[Byte]): ArgWritable = {
    val aw = new ArgWritable
    aw.set(bytes, 0, bytes.length)
    aw.setSize(bytes.length)
    aw
  }
  def apply(aw: ArgWritable): ArgWritable = {
    ArgWritable(aw.getBytes())
  }

  // TODO - make this generic in Array[T]
  def toWritable(array: Array[Int]) = {
    val pixels = new Array[Byte](array.length * IntBytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    val intBuff = bytebuff.asIntBuffer()
    intBuff.put(array)
    ArgWritable(pixels)    
  }
  
  // TODO - make this generic in Array[T]
  def toArray(aw: ArgWritable) = {
    val bytes = aw.getBytes()
    val byteBuffer = ByteBuffer.wrap(bytes, 0, aw.getLength())
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](aw.getLength() / IntBytes)    
    intBuffer.get(intArray)
    intArray
  }
}