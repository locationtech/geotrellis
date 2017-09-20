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

import geotrellis.proj4.io.wkt.WKT

import org.osgeo.proj4j._
import com.github.blemale.scaffeine.Scaffeine

import scala.io.Source


object CRS {
  private lazy val proj4ToEpsgMap =
    Scaffeine()
      .recordStats()
      .build[String, Option[String]]()

  //  new Memoize[String, Option[String]](readEpsgCodeFromFile)
  private val crsFactory = new CRSFactory
  private val filePrefix = "/geotrellis/proj4/nad/"

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
    new CRS {
      val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromParameters(null, proj4Params)

      def epsgCode: Option[Int] = getEpsgCode(toProj4String + " <>")
    }

  /**
    * Returns the numeric EPSG code of a proj4string.
    */
  def getEpsgCode(proj4String: String): Option[Int] =
    proj4ToEpsgMap.get(proj4String, { key => readEpsgCodeFromFile(key) }).map(_.toInt)

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
    new CRS {
      val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromParameters(name, proj4Params)

      def epsgCode: Option[Int] = getEpsgCode(toProj4String + " <>")
    }

  /**
    * Creates a CoordinateReferenceSystem (CRS) from a
    * well-known-text String.
    */
  def fromWKT(wktString: String): CRS = {
    val epsgCode: String = WKT.getEpsgCode(wktString)

    fromName(epsgCode)
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
    new CRS {
      val proj4jCrs: CoordinateReferenceSystem = crsFactory.createFromName(name)

      def epsgCode: Option[Int] = getEpsgCode(toProj4String + " <>")
    }

  /**
    * Creates a CoordinateReferenceSystem (CRS) from an EPSG code.
    */
  def fromEpsgCode(epsgCode: Int) =
    fromName(s"EPSG:$epsgCode")

  private def readEpsgCodeFromFile(proj4String: String): Option[String] = {
    val stream = getClass.getResourceAsStream(s"${filePrefix}epsg")
    try {
      Source.fromInputStream(stream)
        .getLines
        .find { line =>
          !line.startsWith("#") && {
            val proj4Body = line.split("proj")(1)
            s"+proj$proj4Body" == proj4String
          }
        }.flatMap { l =>
          val array = l.split(" ")
          val length = array(0).length
          Some(array(0).substring(1, length - 1))
        }
    } finally {
      stream.close()
    }
  }

  /** Mix-in for singleton CRS implementations where distinguished string should be the name of the object. */
  private[proj4] trait ObjectNameToString { self: CRS ⇒
    override def toString: String = self.getClass.getSimpleName.replaceAllLiterally("$", "")
  }
}


trait CRS extends Serializable {

  val Epsilon = 1e-8

  def epsgCode: Option[Int]

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
  def toWKT(): Option[String] = epsgCode.map(WKT.fromEpsgCode(_))


  // TODO: Do these better once more things are ported
  override
  def hashCode = toProj4String.hashCode

  def toProj4String: String = proj4jCrs.getParameterString

  def isGeographic: Boolean = proj4jCrs.isGeographic

  override
  def equals(o: Any): Boolean =
    o match {
      case other: CRS => compareProj4Strings(other.toProj4String, toProj4String)
      case _ => false
    }

  private def compareProj4Strings(p1: String, p2: String) = {
    def toProj4Map(s: String): Map[String, String] =
      s.trim.split(" ").map(x =>
        if (x.startsWith("+")) x.substring(1) else x).map(x => {
        val index = x.indexOf('=')
        if (index != -1) (x.substring(0, index) -> Some(x.substring(index + 1)))
        else (x -> None)
      }).groupBy(_._1).map { case (a, b) => (a, b.head._2) }
        .filter { case (a, b) => !b.isEmpty }.map { case (a, b) => (a -> b.get) }
        .map { case (a, b) => if (b == "latlong") (a -> "longlat") else (a, b) }
        .filter { case (a, b) => (a != "to_meter" || b != "1.0") }

    val m1 = toProj4Map(p1)
    val m2 = toProj4Map(p2)

    m1.map {
      case (key, v1) => m2.get(key) match {
        case Some(v2) => compareValues(v1, v2)
        case None => false
      }
    }.forall(_ != false)
  }

  private def compareValues(s1: String, s2: String) = {
    def isNumber(s: String) = s.filter(c => !List('.', '-').contains(c)) forall Character.isDigit

    val s2IsNumber = isNumber(s1)
    val s1IsNumber = isNumber(s2)

    if (s1IsNumber == s2IsNumber) {
      if (s1IsNumber) math.abs(s1.toDouble - s2.toDouble) < Epsilon
      else s1 == s2
    } else false
  }

  protected def factory = CRS.crsFactory

  /** Default implementation returns the proj4 name. */
  override def toString: String = this.proj4jCrs.getName
}
