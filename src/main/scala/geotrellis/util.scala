package geotrellis.util

import java.io.{File,FileInputStream}
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode._

import scala.math.min

/**
  * Utility class for timing the execution time of a function.
  */
object Timer {
  def time[T](thunk: => T) = {
    val t0 = System.currentTimeMillis()
    val result = thunk
    val t1 = System.currentTimeMillis()
    (result, t1 - t0)
  }

  def run[T](thunk: => T) = {
    val (result, t) = time { thunk }
    printf("%s: %d ms\n", result, t)
    result
  }

  def log[T](fmt:String, args:Any*)(thunk: => T) = {
    val label = fmt.format(args:_*)
    val (result, t) = time { thunk }
    printf(label + ": %d ms\n".format(t))
    result
  }
}

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

  def slurpToBuffer(path:String, pos:Int, size:Int, bs:Int = 262144) = {
    ByteBuffer.wrap(slurp(path, bs), pos, size)
  }
}
