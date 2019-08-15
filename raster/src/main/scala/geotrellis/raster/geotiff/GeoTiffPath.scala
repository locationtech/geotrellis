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

package geotrellis.raster.geotiff

import geotrellis.raster.SourcePath

import cats.syntax.option._
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.encoding.PercentEncoder
import io.lemonlabs.uri.encoding.PercentEncoder.PATH_CHARS_TO_ENCODE

import java.net.MalformedURLException

/** Represents a VALID path that points to a GeoTiff to be read.
 *  @note The target file must have a file extension.
 *
 *  @param value Path to a GeoTiff. There are two ways to format this `String`: either
 *    in the `URI` format, or as a relative path if the file is local. In addition,
 *    this path can be prefixed with, '''gtiff+''' to signify that the target GeoTiff
 *    is to be read in only by [[GeoTiffRasterSource]].
 *  @example "data/my-data.tiff"
 *  @example "s3://bucket/prefix/data.tif"
 *  @example "gtiff+file:///tmp/data.tiff"
 *
 *  @note Capitalization of the extension is not regarded.
 */
case class GeoTiffPath(value: String) extends SourcePath {
  /** Function that points to the file with external overviews. */
  def externalOverviews: String = s"$value.ovr"
}

object GeoTiffPath {
  val PREFIX = "gtiff+"

  implicit def toGeoTiffDataPath(path: String): GeoTiffPath = parse(path)

  def parseOption(path: String, percentEncoder: PercentEncoder = PercentEncoder(PATH_CHARS_TO_ENCODE ++ Set('%', '?', '#'))): Option[GeoTiffPath] = {
    val upath = percentEncoder.encode(path, "UTF-8")
    Uri.parseOption(upath).fold(Option.empty[GeoTiffPath]) { uri =>
      GeoTiffPath(uri.schemeOption.fold(uri.toStringRaw) { scheme =>
        uri.withScheme(scheme.split("\\+").last).toStringRaw
      }).some
    }
  }

  def parse(path: String, percentEncoder: PercentEncoder = PercentEncoder(PATH_CHARS_TO_ENCODE ++ Set('%', '?', '#'))): GeoTiffPath =
    parseOption(path, percentEncoder).getOrElse(throw new MalformedURLException(s"Unable to parse GeoTiffDataPath: $path"))
}

