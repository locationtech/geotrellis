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

package geotrellis.vector.io.json

import geotrellis.proj4.CRS
import scala.io.Source
import scala.util.Try
import java.net.URI


/** Abstract trait for various implementations of Coordinate Reference System values
  * @note Reference: http://geojson.org/geojson-spec.html#coordinate-reference-system-objects
  */
sealed abstract class JsonCRS(crsType: String) {
  /** Attempt to resolve to [[CRS]] object. Will cause network request for [[LinkedCRS]] */
  def toCRS: Option[CRS]
}

/** A CRS object which indicates a coordinate reference system by name.
  *
  * @param name must be a string identifying a coordinate reference system.
  */
case class NamedCRS(name: String) extends JsonCRS("name") {
  def toCRS: Option[CRS] =
    Try(CRS.fromName(name)).toOption
}

/** A CRS object with a link to CRS parameters on the Web.
  *
  * @param href must be a dereferenceable URI.
  * @param format must be a string that hints at the format
  *        used to represent CRS parameters at the provided URI.
  * @note  Suggested values are: "proj4", "ogcwkt", "esriwkt", others can be used:
  */
case class LinkedCRS(href: URI, format: String) extends JsonCRS("link") {
  def toCRS: Option[CRS] = {
    format match {
      case "proj4" =>
        fetchCrsString.map(CRS.fromString)
      case _ =>
        None
    }
  }

  /** Attempt to read href as URL and read its contents */
  def fetchCrsString: Option[String] = Try {
    val in = href.toURL.openStream()
    try {
      Source.fromInputStream(in).mkString
    } finally {
      in.close()
    }
  }.toOption
}


/** Used as a named tuple to extract and insert CRS field in GeoJSON objects */
case class WithCrs[T](obj: T, crs: JsonCRS)