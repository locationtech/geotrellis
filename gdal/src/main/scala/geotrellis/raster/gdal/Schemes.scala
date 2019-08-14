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

package geotrellis.raster.gdal

/*
 * This object conatins the different schemes and filetypes one can pass
 * into GDAL.
 */
object Schemes {
  final val FTP   = "ftp"
  final val HTTP  = "http"
  final val HTTPS = "https"

  final val TAR  = "tar"
  final val ZIP  = "zip"
  final val GZIP = "gzip"
  final val GZ   = "gz"

  final val FILE = "file"

  final val S3 = "s3"

  final val GS = "gs"

  final val WASB  = "wasb"
  final val WASBS = "wasbs"

  final val HDFS  = "hdfs"

  final val TGZ = "tgz"

  final val KMZ = "kmz"

  final val ODS = "ods"

  final val XLSX = "xlsx"

  final val EMPTY = ""

  final val COMPRESSED_FILE_TYPES = Array(TAR, TGZ, ZIP, KMZ, ODS, XLSX, GZIP, GZ, KMZ)

  final val URI_PROTOCOL_INCLUDE = Array(FTP, HTTP, HTTPS, HDFS)

  final val URI_HOST_EXCLUDE = Array(WASB, WASBS)

  def isCompressed(schemes: String): Boolean =
    COMPRESSED_FILE_TYPES.map(toVSIScheme).collect { case es if es.nonEmpty => schemes.contains(es) }.reduce(_ || _)

  def extraCompressionScheme(path: String): Option[String] =
    COMPRESSED_FILE_TYPES
      .flatMap { ext => if (path.contains(s".$ext")) Some(toVSIScheme(ext)) else None }
      .lastOption

  def isVSIFormatted(path: String): Boolean = path.startsWith("/vsi")

  def toVSIScheme(scheme: String): String = scheme match {
    case FTP | HTTP | HTTPS => "/vsicurl/"
    case S3                 => "/vsis3/"
    case GS                 => "/vsigs/"
    case WASB | WASBS       => "/vsiaz/"
    case HDFS               => "/vsihdfs/"
    case ZIP | KMZ          => "/vsizip/"
    case GZ | GZIP          => "/vsigzip/"
    case TAR | TGZ          => "/vsitar/"
    case _                  => ""
  }
}
