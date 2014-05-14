/* 
 * Copyright (c) 2013, Minglei Tu (tmlneu@gmail.com)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package geotrellis.slick

import scala.slick.driver.JdbcDriver
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import scala.slick.ast.{ScalaBaseType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}

import geotrellis.feature._
import geotrellis.feature.io._

/** 
 * This class provides column types and extension methods to work with Geometry columns in PostGIS.
 *
 * Sample Usage: 
 * <code>
 * val PostGIS = new PostGisSupport(PostgresDriver)
 * import PostGIS._
 * 
 * class City(tag: Tag) extends Table[(Int,String,Point)](tag, "cities") {      
 *   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
 *   def name = column[String]("name")
 *   def geom = column[Point]("geom")
 *   def * = (id, name, geom)
 * }
 * </code>
 *
 * based on [[package com.github.tminglei.slickpg.PgPostGISSupport]]
 */
class PostGisSupport(override val driver: JdbcDriver) extends PostGisExtensions { 
  import PostGisSupportUtils._

  type GEOMETRY           = Geometry
  type POINT              = Point
  type LINESTRING         = Line
  type POLYGON            = Polygon
  type GEOMETRYCOLLECTION = GeometryCollection

  implicit val geometryTypeMapper = new GeometryJdbcType[GEOMETRY]
  implicit val pointTypeMapper = new GeometryJdbcType[POINT]
  implicit val lineTypeMapper = new GeometryJdbcType[LINESTRING]
  implicit val polygonTypeMapper = new GeometryJdbcType[POLYGON]
  implicit val geometryCollectionTypeMapper = new GeometryJdbcType[GEOMETRYCOLLECTION]  
  implicit val multiPointTypeMapper = new GeometryJdbcType[MultiPoint]
  implicit val multiPolygonTypeMapper = new GeometryJdbcType[MultiPolygon]
  implicit val multiLineTypeMapper = new GeometryJdbcType[MultiLine]

  implicit def geometryColumnExtensionMethods[G1 <: GEOMETRY](c: Column[G1]) = 
    new GeometryColumnExtensionMethods[G1, G1](c)
  
  implicit def geometryOptionColumnExtensionMethods[G1 <: GEOMETRY](c: Column[Option[G1]]) = 
    new GeometryColumnExtensionMethods[G1, Option[G1]](c)

  
  class GeometryJdbcType[T <: Geometry : ClassTag] extends driver.DriverJdbcType[T] {
    override def scalaType = ScalaBaseType[T]
    
    override def sqlTypeName: String = "geometry"
    
    override def hasLiteralForm: Boolean = false
    
    override def valueToSQLLiteral(v: T) = toLiteral(v)

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def setValue(v: T, p: PositionedParameters) = p.setBytes(WKB.write(v))

    def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral[T]).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateBytes(WKB.write(v))
  }
}

object PostGisSupportUtils {  
  def toLiteral(geom: Geometry): String = WKT.write(geom)

  def fromLiteral[T <: Geometry](value: String): T = {
    splitRSIDAndWKT(value) match {
      case (srid, wkt) => { //TODO - SRID is ignored
        if (wkt.startsWith("00") || wkt.startsWith("01"))
          WKB.read[T](wkt)
        else 
          WKT.read[T](wkt)
      }
    }
  }

  /** copy from [[org.postgis.PGgeometry#splitSRID]] */
  private def splitRSIDAndWKT(value: String): (Int, String) = {
    if (value.startsWith("SRID=")) {
      val index = value.indexOf(';', 5) // srid prefix length is 5
      if (index == -1) {
        throw new java.sql.SQLException("Error parsing Geometry - SRID not delimited with ';' ")
      } else {
        val srid = Integer.parseInt(value.substring(0, index))
        val wkt = value.substring(index + 1)
        (srid, wkt)
      }
    } else (-1, value)
  }
}