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

package geotrellis.proj4

import geotrellis.proj4.io.wkt.{ExtensionProj4, ProjCS, WKT, WKTParser}
import org.locationtech.proj4j._
import org.locationtech.proj4j.util.CRSCache

import scala.util.Try

object CRS {
  private val crsFactory = new CRSCache()

  /**
    * Creates a CoordinateReferenceSystem
    * from a PROJ.4 projection parameter string.
    * <p>
    * An example of a valid PROJ.4 projection parameter string is:
    * <pre>
    * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
    * </pre>
    *
    * @param  proj4Params  A PROJ.4 projection parameter string
    * @return              The specified CoordinateReferenceSystem
    */
  def fromString(proj4Params: String): CRS =
    new CRS { val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromParameters(null, proj4Params) }

  /**
    * Returns the numeric EPSG code of a proj4string.
    */
  def getEpsgCode(proj4String: String): Option[Int] = readEpsgFromParameters(proj4String).map(_.toInt)

  /**
    * Creates a CoordinateReferenceSystem
    * from a PROJ.4 projection parameter string.
    * <p>
    * An example of a valid PROJ.4 projection parameter string is:
    * <pre>
    * +proj=aea +lat_1=50 +lat_2=58.5 +lat_0=45 +lon_0=-126 +x_0=1000000 +y_0=0 +ellps=GRS80 +units=m
    * </pre>
    *
    * @param  name         A name for this coordinate system.
    * @param  proj4Params  A PROJ.4 projection parameter string
    * @return              The specified CoordinateReferenceSystem
    */
  def fromString(name: String, proj4Params: String): CRS =
    new CRS { val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromParameters(name, proj4Params) }

  /**
    * Creates a CoordinateReferenceSystem (CRS) from a
    * well-known-text String.
    */
  def fromWKT(wktString: String): Option[CRS] = {
    val fromEpsgCode = WKT.getEpsgStringCode(wktString).map(fromName)
    if(fromEpsgCode.isEmpty) {
      WKTParser.parse(wktString).toOption.flatMap {
        case wkt: ProjCS =>
          wkt.extension.flatMap {
            case ExtensionProj4(proj4String) =>
              Some(CRS.fromString(proj4String))
            case _ => None
          }
        case _ => fromEpsgCode
      }
    } else fromEpsgCode
  }

  /**
    * Creates a CoordinateReferenceSystem (CRS) from a well-known name.
    * CRS names are of the form: "<tt>authority:code</tt>",
    * with the components being:
    * <ul>
    * <li><b><tt>authority</tt></b> is a code for a namespace supported by
    * PROJ.4.
    * Currently supported values are
    * <tt>EPSG</tt>,
    * <tt>ESRI</tt>,
    * <tt>WORLD</tt>,
    * <tt>NAD83</tt>,
    * <tt>NAD27</tt>.
    * If no authority is provided, the <tt>EPSG</tt> namespace is assumed.
    * <li><b><tt>code</tt></b> is the id of a coordinate system in the authority namespace.
    * For example, in the <tt>EPSG</tt> namespace a code is an integer value
    * which identifies a CRS definition in the EPSG database.
    * (Codes are read and handled as strings).
    * </ul>
    * An example of a valid CRS name is <tt>EPSG:3005</tt>.
    * <p>
    *
    * @param   name  The name of a coordinate system, with optional authority prefix
    * @return        The CoordinateReferenceSystem corresponding to the given name
   */
  def fromName(name: String): CRS =
    new CRS { val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromName(name) }

  /**
    * Creates a CoordinateReferenceSystem (CRS) from an EPSG code.
    */
  def fromEpsgCode(epsgCode: Int) = fromName(s"EPSG:$epsgCode")

  def readEpsgFromParameters(proj4String: String): Option[String] =
    Option(crsFactory.readEpsgFromParameters(proj4String))

  /** Mix-in for singleton CRS implementations where distinguished string should be the name of the object. */
  private[proj4] trait ObjectNameToString { self: CRS ⇒
    override def toString: String = self.getClass.getSimpleName.replaceAllLiterally("$", "")
  }
}


trait CRS extends Serializable {

  val Epsilon = 1e-8

  def epsgCode: Option[Int] =
    proj4jCrs.getName.split(":") match {
      case Array(name, code) if name.toUpperCase == "EPSG" => Try(code.toInt).toOption
      case _ => CRS.getEpsgCode(toProj4String)
    }

  def proj4jCrs: CoordinateReferenceSystem

  /**
    * Override this function to handle reprojecting to another CRS in
    * a more performant way.
    */
  def alternateTransform(dest: CRS): Option[(Double, Double) => (Double, Double)] =
    None

  /**
   * Returns the WKT representation of the Coordinate Reference
   * System.
   */
  def toWKT(): Option[String] = epsgCode.flatMap(WKT.fromEpsgCode(_))


  // TODO: Do these better once more things are ported
  override def hashCode = toProj4String.hashCode

  def toProj4String: String = proj4jCrs.getParameterString

  def isGeographic: Boolean = proj4jCrs.isGeographic

  override def equals(o: Any): Boolean =
    o match {
      case other: CRS => proj4jCrs == other.proj4jCrs
      case _ => false
    }

  protected def factory = CRS.crsFactory

  /** Default implementation returns the proj4 name. */
  override def toString: String = this.proj4jCrs.getName
}
