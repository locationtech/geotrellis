/*
 * Copyright 2019 Azavea
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

package geotrellis.store.hadoop

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.store.hadoop.util._
import geotrellis.util.MethodExtensions
import geotrellis.vector.io.json.CrsFormats

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce._

import java.io.{ByteArrayOutputStream, ObjectOutputStream, ByteArrayInputStream, ObjectInputStream}
import java.util.Base64

object Implicits extends Implicits with CrsFormats

trait Implicits {
  implicit class withJpgHadoopWriteMethods(val self: Jpg) extends JpgHadoopWriteMethods(self)

  implicit class withPngHadoopWriteMethods(val self: Png) extends PngHadoopWriteMethods(self)

  implicit class withGeoTiffHadoopWriteMethods[T <: CellGrid[Int]](val self: GeoTiff[T]) extends GeoTiffHadoopWriteMethods[T](self)

  implicit class withHadoopConfigurationMethods(val self: Configuration) extends MethodExtensions[Configuration] {
    def modify(f: Job => Unit): Configuration = {
      val job = Job.getInstance(self)
      f(job)
      job.getConfiguration
    }

    def withInputPath(path: Path): Configuration =
      modify(FileInputFormat.addInputPath(_, path))

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, self)
      if(allFiles.isEmpty) {
        sys.error(s"$path contains no files.")
      }
      HdfsUtils.putFilesInConf(allFiles.mkString(","), self)
    }

    /** Creates a configuration with a given directory, to search for all files
      * with an extension contained in the given set of extensions */
    def withInputDirectory(path: Path, extensions: Seq[String]): Configuration = {
      val searchPath = path.toString match {
        case p if extensions.exists(p.endsWith) => path
        case p =>
          val extensionsStr = extensions.mkString("{", ",", "}")
          new Path(s"$p/*$extensionsStr")
      }

      withInputDirectory(searchPath)
    }

    def setSerialized[T](key: String, value: T): Unit = {
      val bos = new ByteArrayOutputStream()
      val oos =  new ObjectOutputStream(bos)
      oos.writeObject(value)
      val bytes = bos.toByteArray()
      oos.close()
      val str = Base64.getEncoder().encodeToString(bytes);
      self.set(key, str)
    }

    def getSerialized[T](key: String): T = {
        val str = self.get(key)
        val bytes = Base64.getDecoder().decode(str)
      val bis = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bis)
      val value = ois.readObject()
      ois.close()
      value.asInstanceOf[T]
    }

    def getSerializedOption[T](key: String): Option[T] = {
      val s = self.get(key)
      if(s == null) {
        None
      } else {
        val str = self.get(key)
        val bytes = Base64.getDecoder().decode(str)
        val bis = new ByteArrayInputStream(bytes)
        val ois = new ObjectInputStream(bis)
        val value = ois.readObject()
        ois.close()
        Some(value.asInstanceOf[T])
      }
    }
  }
}
