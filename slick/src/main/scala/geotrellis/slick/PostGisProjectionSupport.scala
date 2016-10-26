/*
 * Copyright (c) 2013, Minglei Tu (tmlneu@gmail.com)
 * Copyright (c) 2015, Azavea
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

import geotrellis.vector._
import geotrellis.vector.io.wkb._
import geotrellis.vector.io.wkt._

import slick.ast.FieldSymbol
import slick.driver.{JdbcDriver, PostgresDriver}
import slick.jdbc.{PositionedParameters, PositionedResult, SetParameter}
import com.github.tminglei.slickpg.geom.PgPostGISExtensions

import java.sql._
import scala.reflect.ClassTag

/**
 * This class provides column types and extension methods to work with Geometry columns
 *  associated with an SRID in PostGIS.
 *
 * Sample Usage:
 * <code>
 * val PostGIS = new PostGisProjectionSupport(PostgresDriver)
 * import PostGIS._
 *
 * class City(tag: Tag) extends Table[(Int,String,Projected[Point])](tag, "cities") {
 *   def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
 *   def name = column[String]("name")
 *   def geom = column[Projected[Point]]("geom")
 *   def * = (id, name, geom)
 * }
 * </code>
 *
 * based on [[package com.github.tminglei.slickpg.PgPostGISSupport]]
 */
trait PostGisProjectionSupport extends PgPostGISExtensions { driver: PostgresDriver =>
  import PostGisProjectionSupportUtils._
  import driver.api._

  type GEOMETRY           = Projected[Geometry]
  type POINT              = Projected[Point]
  type LINESTRING         = Projected[Line]
  type POLYGON            = Projected[Polygon]
  type GEOMETRYCOLLECTION = Projected[GeometryCollection]

  trait PostGISProjectionAssistants extends BasePostGISAssistants[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION]
trait PostGISProjectionImplicits {
  implicit val geometryTypeMapper = new ProjectedGeometryJdbcType[GEOMETRY]
  implicit val pointTypeMapper = new ProjectedGeometryJdbcType[POINT]
  implicit val lineTypeMapper = new ProjectedGeometryJdbcType[LINESTRING]
  implicit val polygonTypeMapper = new ProjectedGeometryJdbcType[POLYGON]
  implicit val geometryCollectionTypeMapper = new ProjectedGeometryJdbcType[GEOMETRYCOLLECTION]
  implicit val multiPointTypeMapper = new ProjectedGeometryJdbcType[Projected[MultiPoint]]
  implicit val multiPolygonTypeMapper = new ProjectedGeometryJdbcType[Projected[MultiPolygon]]
  implicit val multiLineTypeMapper = new ProjectedGeometryJdbcType[Projected[MultiLine]]

  implicit def geometryColumnExtensionMethods[G1 <: GEOMETRY](c: Rep[G1]) =
    new GeometryColumnExtensionMethods[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION, G1, G1](c)

  implicit def geometryOptionColumnExtensionMethods[G1 <: GEOMETRY](c: Rep[Option[G1]]) =
    new GeometryColumnExtensionMethods[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION, G1, Option[G1]](c)
}

  class ProjectedGeometryJdbcType[T <: Projected[Geometry] :ClassTag] extends DriverJdbcType[T] {

    override def sqlTypeName(sym: Option[FieldSymbol]): String = "geometry"

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(v: T) = toLiteral(v)

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def setValue(v: T, p: PreparedStatement, idx: Int) = p.setBytes(idx, WKB.write(v.geom, v.srid))

    def updateValue(v: T, r: ResultSet, idx: Int) = r.updateBytes(idx, WKB.write(v.geom, v.srid))

    def getValue(r: ResultSet, idx: Int): T = {
      val s = r.getString(idx)
      (if(r.wasNull) None else Some(s))
        .map(fromLiteral[T](_))
        .getOrElse(zero)
    }
  }
}

object PostGisProjectionSupportUtils {
  lazy val WITH_SRID = """^SRID=([\d]+);(.*)""".r

  def toLiteral(pg: Projected[Geometry]): String = s"SRID=${pg.srid};${WKT.write(pg.geom)}"

  def fromLiteral[T <: Projected[_]](value: String): T =
    value match {
      case WITH_SRID(srid, wkt) =>
        val geom = readWktOrWkb(wkt)
        Projected(geom, srid.toInt).asInstanceOf[T]
      case _ =>
        val geom = readWktOrWkb(value)
        Projected(geom, geom.jtsGeom.getSRID).asInstanceOf[T]
    }

  def readWktOrWkb(s: String): Geometry =
    if (s.startsWith("00") || s.startsWith("01"))
      WKB.read(s)
    else
      WKT.read(s)
}
