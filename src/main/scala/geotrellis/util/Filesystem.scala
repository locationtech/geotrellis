package geotrellis.util

import java.io.{File,FileInputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._
import scala.math.min

object Filesystem {
  def slurp(path:String, bs:Int = 262144):Array[Byte] = {
    val f = new File(path)
    val fis = new FileInputStream(f)
    val size = f.length.toInt
    val channel = fis.getChannel
    val buffer = channel.map(READ_ONLY, 0, size)
    fis.close
    
    // read 256K at a time out of the buffer into our array
    var i = 0
    val data = Array.ofDim[Byte](size)
    while(buffer.hasRemaining()) {
      val n = min(buffer.remaining(), bs)
      buffer.get(data, i, n)
      i += n
    }

    data
  }

  /**
   * Return the path string with the final extension removed.
   */
  def basename(p:String) = p.lastIndexOf(".") match {
    case -1 => p
    case n => p.substring(0, n)
  }

  def split(p:String) = p.lastIndexOf(".") match {
    case -1 => (p, "")
    case n => (p.substring(0, n), p.substring(n + 1, p.length))
  }

  def slurpToBuffer(path:String, pos:Int, size:Int, bs:Int = 262144) = {
    ByteBuffer.wrap(slurp(path, bs), pos, size)
  }

  def join(parts:String*) = parts.mkString(File.separator)
}
