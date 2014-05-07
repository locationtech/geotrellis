package geotrellis.slick

import scala.slick.driver._
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}

import com.vividsolutions.jts.{geom => jts}
import geotrellis.feature._
import geotrellis.feature.io._


/** copy from [[package com.github.tminglei.slickpg.PgPostGISSupport]] */
trait PostGisProjectionSupport extends PostGisExtensions { driver: JdbcDriver =>
  import PostGisProjectionSupportUtils._

  type GEOMETRY           = ProjectedGeometry
  type POINT              = ProjectedPoint
  type LINESTRING         = ProjectedLine
  type POLYGON            = ProjectedPolygon
  type GEOMETRYCOLLECTION = ProjectedGeometryCollection

  trait PostGisImplicits {
    implicit val geometryTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry]
    implicit val pointTypeMapper = new ProjectedGeometryJdbcType[ProjectedPoint]
    implicit val polygonTypeMapper = new ProjectedGeometryJdbcType[ProjectedPolygon]
    implicit val lineTypeMapper = new ProjectedGeometryJdbcType[ProjectedLine]
    implicit val geometryCollectionTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometryCollection]
    // implicit val multiPointTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiPoint], MultiPoint]
    // implicit val multiPolygonTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiPolygon], MultiPolygon]
    // implicit val multiLineTypeMapper = new ProjectedGeometryJdbcType[ProjectedGeometry[MultiLine], MultiLine]

    implicit def geometryColumnExtensionMethods[G1 <: ProjectedGeometry](c: Column[G1]) = 
      new GeometryColumnExtensionMethods[G1, G1](c)
    
    implicit def geometryOptionColumnExtensionMethods[G1 <: ProjectedGeometry](c: Column[Option[G1]]) = 
      new GeometryColumnExtensionMethods[G1, Option[G1]](c)
  }

  class ProjectedGeometryJdbcType[T <: ProjectedGeometry :ClassTag] extends JdbcType[T] with BaseTypedType[T] {

    def scalaType: ScalaType[T] = ScalaBaseType[T]

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = "geometry"

    def setValue(v: T, p: PositionedParameters) = p.setBytes(WKB.write(v.geom, v.srid))

    def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral[T]).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateBytes(WKB.write(v.geom, v.srid))

    def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(v: T) = toLiteral(v)
  }
}

object PostGisProjectionSupportUtils {  
  def toLiteral(pg: ProjectedGeometry): String = WKT.write(pg.geom)

  def fromLiteral[T <: ProjectedGeometry](value: String): T = 
    splitRSIDAndWKT(value) match {
      case (srid, wkt) =>
        val geom =
          if (wkt.startsWith("00") || wkt.startsWith("01"))
            WKB.read[Geometry](wkt)
          else 
            WKT.read[Geometry](wkt)

        ProjectedGeometry(geom, srid).asInstanceOf[T]
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