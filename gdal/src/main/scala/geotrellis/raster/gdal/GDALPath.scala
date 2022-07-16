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

import geotrellis.raster.SourcePath

import cats.syntax.option._
import io.lemonlabs.uri._
import io.lemonlabs.uri.encoding.PercentEncoder
import io.lemonlabs.uri.encoding.PercentEncoder.PATH_CHARS_TO_ENCODE
import java.net.MalformedURLException

/** Represents and formats a path that points to a files to be read by GDAL.
 *
 *  @param value Path to the file. This path can be formatted in the following
 *    styles: `VSI`, `URI`, or relative path if the file is local. In addition,
 *    this path can be prefixed with, '''gdal+''' to signify that the target GeoTiff
 *    is to be read in only by [[GDALRasterSource]].
 *  @example "/vsizip//vsicurl/http://localhost:8000/files.zip"
 *  @example "s3://bucket/prefix/data.tif"
 *  @example "gdal+file:///tmp/data.tiff"
 *  @note Under normal usage, GDAL requires that all paths to be read be given in its
 *    `VSI Format`. Thus, if given another format type, this class will format it
 *    so that it can be read.
 *
 *  @example "zip+s3://bucket/prefix/zipped-data.zip!data.tif"
 */
case class GDALPath(value: String) extends SourcePath

object GDALPath {
  val PREFIX = "gdal+"

  /* This object conatins the different schemes and filetypes one can pass into GDAL */
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

  implicit def toGDALDataPath(path: String): GDALPath = GDALPath.parse(path)

  def parseOption(
    path: String,
    compressedFileDelimiter: Option[String] = "!".some,
    percentEncoder: PercentEncoder = PercentEncoder(PATH_CHARS_TO_ENCODE ++ Set('%', '?', '#'))
  ): Option[GDALPath] = {
    import Schemes._

    // Trying to read something locally on Windows matters
    // because of how file paths on Windows are formatted.
    // Therefore, we need to handle them differently.
    val onLocalWindows = System.getProperty("os.name").toLowerCase == "win"
    val upath = percentEncoder.encode(path, "UTF-8")

    val vsiPath: Option[String] =
      if (isVSIFormatted(path)) path.some
      else
        UrlWithAuthority
          .parseOption(upath)
          // try to parse it, otherwise it is a path
          .fold((Url().withPath(UrlPath.fromRaw(upath)): Url).some)(_.some)
          .flatMap { url =>
            // authority is an optional thing and required only for Azure
            val authority =
              url match {
                case url: UrlWithAuthority => url.authority.user.getOrElse(EMPTY)
                case _ => EMPTY
              }

            // relative path, scheme and charecters should be percent decoded
            val relativeUrl = url.toRelativeUrl.path.toStringRaw

            // it can also be the case that there is no scheme (the Path case)
            url.schemeOption.fold(EMPTY.some)(_.some).map { scheme =>
              val schemesArray = scheme.split("\\+")
              val schemes = schemesArray.map(toVSIScheme).mkString

              // reverse slashes are used on windows for zip files paths
              val path =
                (if (schemes.contains(FILE) && onLocalWindows) compressedFileDelimiter.map(relativeUrl.replace(_, """\\"""))
                else compressedFileDelimiter.map(relativeUrl.replace(_, "/"))).getOrElse(relativeUrl)

              // check out the last .${extension}, probably we need auto add it into the vsipath construction
              val extraScheme = extraCompressionScheme(path)

              // check out that we won't append a vsi path duplicate or other compression vsipath
              val extraSchemeExists = extraScheme.exists { es => schemes.nonEmpty && (schemes.contains(es) || isCompressed(schemes)) }

              val extendedSchemes = extraScheme.fold(schemes) {
                case _ if extraSchemeExists => schemes
                case str => s"$str$schemes"
              }

              // in some cases scheme:// should be added after the vsi path protocol, sometimes not
              val webProtocol = schemesArray.collectFirst { case sch if URI_PROTOCOL_INCLUDE.contains(sch) => s"$sch://" }.getOrElse(EMPTY)

              url
                .hostOption
                .filterNot(_ => URI_HOST_EXCLUDE.map(schemesArray.contains).reduce(_ || _)) // filter the host out, for instance in the Azure case
                .fold(s"$extendedSchemes$webProtocol$authority$path")(host => s"$extendedSchemes$webProtocol$authority$host$path")
            }
          }

    vsiPath.map(GDALPath(_))
  }

  def parse(
    path: String,
    compressedFileDelimiter: Option[String] = "!".some,
    percentEncoder: PercentEncoder = PercentEncoder((PATH_CHARS_TO_ENCODE - '/') ++ Set('%', '?', '#'))
  ): GDALPath =
    parseOption(path, compressedFileDelimiter, percentEncoder)
      .getOrElse(throw new MalformedURLException(s"Unable to parse GDALDataPath: $path"))
}
