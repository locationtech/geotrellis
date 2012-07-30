package geotrellis.util

import java.io.{File,FileInputStream}
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel.MapMode._

import scala.math.min

object Units {
  val bytesUnits = List("B", "K", "M", "G", "P")
  def bytes(n:Long) = {
    def xyz(amt:Double, units:List[String]): (Double, String) = units match {
      case Nil => sys.error("invalid units list")
      case u :: Nil => (amt, u)
      case u :: us => if (amt < 1024) (amt, u) else xyz(amt / 1024, us)
    }
    xyz(n, bytesUnits)
  }  
}

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

  //def findFiles(f:File, r:Regex):Array[File] = {
  //  val these = f.listFiles
  //  val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
  //  good ++ these.filter(_.isDirectory).flatMap(recursiveListFiles(_, r))
  //}
}
