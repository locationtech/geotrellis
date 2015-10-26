package geotrellis.spark.io

import java.io.{File, FileInputStream, FileOutputStream}

import org.apache.commons.io.IOUtils
import geotrellis.spark.utils.cache._

class FileCache(cacheDirectory: String, fileChunk: Long => String) extends CacheStrategy[Long, Array[Byte]] {
  val cacheRoot = new File(cacheDirectory)
  require(cacheRoot.isDirectory, s"$cacheRoot must be a directory")
  cacheRoot.mkdirs()

  private def getPath(k: Long) = new File(cacheDirectory, fileChunk(k))

  def lookup(k: Long):Option[Array[Byte]] = {
    val path = getPath(k)
    if (path.exists)
      Some(IOUtils.toByteArray(new FileInputStream(path)))
    else
      None
  }

  def insert(k: Long, v: Array[Byte]):Boolean = {
    val path = getPath(k)
    val cacheOut = new FileOutputStream(path)
    try {cacheOut.write(v); true}
    catch { case e: Exception => false }
    finally { cacheOut.close() }
  }

  def remove(k: Long):Option[Array[Byte]] = {
    val ans = lookup(k)
    if (ans.isDefined) getPath(k).delete()
    ans
  }
}