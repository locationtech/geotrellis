/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.util.cache

import java.io.{File, FileInputStream, FileOutputStream}

import org.apache.commons.io.IOUtils

@deprecated("This will be removed in favor of a pluggable cache in 2.0", "1.2")
class FileCache(cacheDirectory: String, fileChunk: Long => String) extends Cache[Long, Array[Byte]] {
  val cacheRoot = new File(cacheDirectory)
  if (! cacheRoot.exists) cacheRoot.mkdirs()
  require(cacheRoot.isDirectory, s"$cacheRoot must be a directory")


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
