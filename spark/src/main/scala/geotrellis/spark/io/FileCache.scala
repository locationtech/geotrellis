package geotrellis.spark.io

import java.io.{File, FileInputStream, FileOutputStream}

import org.apache.commons.io.IOUtils

class FileCache(cacheDirectory: String, fileChunk: Long => String) extends Cache[Long, Array[Byte]] {
  val cacheRoot = new File(cacheDirectory)
  require(cacheRoot.isDirectory, s"$cacheRoot must be a directory")

  def contains(key: Long) = {
    new File(cacheDirectory, fileChunk(key)).exists
  }

  def update(key: Long, value: Array[Byte]) = {
    val path = new File(cacheDirectory, fileChunk(key))
    val cacheOut = new FileOutputStream(path)
    try { cacheOut.write(value) }
    finally { cacheOut.close() }
  }

  def apply(key: Long) = {
    val path = new File(cacheDirectory, fileChunk(key))
    if (path.exists)
      Some(IOUtils.toByteArray(new FileInputStream(path)))
    else
      None
  }
}