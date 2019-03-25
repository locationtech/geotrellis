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

import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

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
trait PostGisSupport extends PgPostGISExtensions { driver: PostgresDriver =>
  import PostGisSupportUtils._
  import driver.api._

  type GEOMETRY           = Geometry
  type POINT              = Point
  type LINESTRING         = Line
  type POLYGON            = Polygon
  type GEOMETRYCOLLECTION = GeometryCollection

  trait PostGISAssistants extends BasePostGISAssistants[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION]
  trait PostGISImplicits {
    implicit val geometryTypeMapper = new GeometryJdbcType[GEOMETRY]
    implicit val pointTypeMapper = new GeometryJdbcType[POINT]
    implicit val lineTypeMapper = new GeometryJdbcType[LINESTRING]
    implicit val polygonTypeMapper = new GeometryJdbcType[POLYGON]
    implicit val geometryCollectionTypeMapper = new GeometryJdbcType[GEOMETRYCOLLECTION]
    implicit val multiPointTypeMapper = new GeometryJdbcType[MultiPoint]
    implicit val multiPolygonTypeMapper = new GeometryJdbcType[MultiPolygon]
    implicit val multiLineTypeMapper = new GeometryJdbcType[MultiLine]

    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Rep[G1]) =
      new GeometryColumnExtensionMethods[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION, G1, G1](c)

    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Rep[Option[G1]]) =
      new GeometryColumnExtensionMethods[GEOMETRY, POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION, G1, Option[G1]](c)
  }

  class GeometryJdbcType[T <: Geometry](implicit override val classTag: ClassTag[T]) extends DriverJdbcType[T]{

    override def sqlTypeName(sym: Option[FieldSymbol]): String = "geometry"

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(v: T) = toLiteral(v)

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def setValue(v: T, p: PreparedStatement, idx: Int) = p.setBytes(idx, WKB.write(v))

    def updateValue(v: T, r: ResultSet, idx: Int) = r.updateBytes(idx, WKB.write(v))

    def getValue(r: ResultSet, idx: Int): T = {
      val s = r.getString(idx)
      (if(r.wasNull) None else Some(s))
        .map(fromLiteral[T](_))
        .getOrElse(zero)
    }
  }
}

object PostGisSupportUtils {
  import PostGisProjectionSupportUtils._

  def toLiteral(geom: Geometry): String = WKT.write(geom)

  def fromLiteral[T <: Geometry](value: String): T = {
    val wkt =
      value match {
        case WITH_SRID(srid, wkt) => wkt
        case _ => value
      }

    readWktOrWkb(wkt).asInstanceOf[T]
  }
}
