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

package geotrellis.slick

import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT
import geotrellis.vector.io.wkb.WKB

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

  def readWktOrWkb(s: String): Geometry = {
    if (s.startsWith("\\x"))
      WKB.read(s.drop(2))
    else if (s.startsWith("00") || s.startsWith("01"))
      WKB.read(s)
    else
      WKT.read(s)
  }
}
