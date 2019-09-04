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

package geotrellis.store

import geotrellis.raster.SourcePath

import cats.syntax.option._
import io.lemonlabs.uri.{Url, UrlPath, UrlWithAuthority}

import java.net.MalformedURLException

/** Represents a path that points to a GeoTrellis layer saved in a catalog.
 *
 *  @param value Path to the layer. This can be either an Avro or COG layer.
 *    The given path needs to be in a `URI` format that include the following query
 *    parameters:
 *      - '''layer''': The name of the layer.
 *      - '''zoom''': The zoom level to be read.
 *      - '''band_count''': The number of bands of each Tile in the layer.
 *    Of the above three parameters, `layer` and `zoom` are required. In addition,
 *    this path can be prefixed with, '''gt+''' to signify that the target path
 *    is to be read in only by [[GeotrellisRasterSource]].
 *  @example "s3://bucket/catalog?layer=layer_name&zoom=10"
 *  @example "hdfs://data-folder/catalog?layer=name&zoom-12&band_count=5"
 *  @example "gt+file:///tmp/catalog?layer=name&zoom=5"
 *  @note The order of the query parameters does not matter.
 */
case class GeoTrellisPath(value: String, layerName: String, zoomLevel: Option[Int], bandCount: Option[Int]) extends SourcePath {
  def  layerId: LayerId = LayerId(layerName, zoomLevel.get)
}

object GeoTrellisPath {
  val PREFIX = "gt+"

  implicit def toGeoTrellisDataPath(path: String): GeoTrellisPath = parse(path)

  def parseOption(path: String): Option[GeoTrellisPath] = {
    val layerNameParam: String = "layer"
    val zoomLevelParam: String = "zoom"
    val bandCountParam: String = "band_count"

    // try to parse it, otherwise it is a path
    val uri = UrlWithAuthority.parseOption(path).fold(Url().withPath(UrlPath.fromRaw(path)): Url)(identity)
    val queryString = uri.query

    val catalogPath: Option[String] = {
      uri.schemeOption.fold(uri.toStringRaw.some) { scheme =>
        val authority =
          uri match {
            case url: UrlWithAuthority => url.authority.userInfo.user.getOrElse("")
            case _ => ""
          }

        s"${scheme.split("\\+").last}://$authority${uri.path}".some
      }
    }

    catalogPath.fold(Option.empty[GeoTrellisPath]) { catalogPath =>
      val layerName: Option[String] = queryString.param(layerNameParam)
      val zoomLevel: Option[Int] = queryString.param(zoomLevelParam).map(_.toInt)
      val bandCount: Option[Int] = queryString.param(bandCountParam).map(_.toInt)

      layerName.map(GeoTrellisPath(catalogPath, _, zoomLevel, bandCount))
    }
  }

  def parse(path: String): GeoTrellisPath =
    parseOption(path).getOrElse(throw new MalformedURLException(s"Unable to parse GeoTrellisDataPath: $path"))
}
