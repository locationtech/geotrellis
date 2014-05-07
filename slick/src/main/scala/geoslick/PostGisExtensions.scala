package geotrellis.slick

import scala.slick.lifted._
import scala.slick.ast.{LiteralNode}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.driver.{JdbcDriver, JdbcTypesComponent, PostgresDriver}
import scala.slick.lifted.FunctionSymbolExtensionMethods._

 /** copy from [[package com.github.tminglei.slickpg.PgPostGISExtensions]] */
trait PostGisExtensions extends JdbcTypesComponent { driver: JdbcDriver =>
  import driver.Implicit._

  type GEOMETRY
  type POINT <: GEOMETRY
  type LINESTRING <: GEOMETRY
  type POLYGON <: GEOMETRY
  type GEOMETRYCOLLECTION <: GEOMETRY

  trait PostGisAssistants {
    /** Geometry Constructors */
    def geomFromText[P, R](wkt: Column[P], srid: Option[Int] = None)(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) =
      srid match {
        case Some(srid) => om.column(GeomLibrary.GeomFromText, wkt.toNode, LiteralNode(srid))
        case None   => om.column(GeomLibrary.GeomFromText, wkt.toNode)
      }
    def geomFromWKB[P, R](wkb: Column[P], srid: Option[Int] = None)(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[Array[Byte], P]#to[GEOMETRY, R]) =
      srid match {
        case Some(srid) => om.column(GeomLibrary.GeomFromWKB, wkb.toNode, LiteralNode(srid))
        case None   => om.column(GeomLibrary.GeomFromWKB, wkb.toNode)
      }
    def geomFromEWKT[P, R](ewkt: Column[P])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.GeomFromEWKT, ewkt.toNode)
      }
    def geomFromEWKB[P, R](ewkb: Column[P])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[Array[Byte], P]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.GeomFromEWKB, ewkb.toNode)
      }
    def geomFromGML[P, R](gml: Column[P], srid: Option[Int] = None)(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) =
      srid match {
        case Some(srid) => om.column(GeomLibrary.GeomFromGML, gml.toNode, LiteralNode(srid))
        case None   => om.column(GeomLibrary.GeomFromGML, gml.toNode)
      }
    def geomFromKML[P, R](kml: Column[P])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.GeomFromKML, kml.toNode)
      }
    def geomFromGeoJSON[P, R](json: Column[P])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.GeomFromGeoJSON, json.toNode)
      }
    def makeBox[G1 <: GEOMETRY, P1, G2 <: GEOMETRY, P2, R](lowLeftPoint: Column[P1], upRightPoint: Column[P2])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[G1, P1]#arg[G2, P2]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.MakeBox, lowLeftPoint.toNode, upRightPoint.toNode)
      }
    def makeBox3d[G1 <: GEOMETRY, P1, G2 <: GEOMETRY, P2, R](lowLeftPoint: Column[P1], upRightPoint: Column[P2])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[G1, P1]#arg[G2, P2]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.MakeBox3D, lowLeftPoint.toNode, upRightPoint.toNode)
      }
    def makeEnvelope(xmin: Column[Double], ymin: Column[Double], xmax: Column[Double], ymax: Column[Double], srid: Option[Int] = None)(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[Double, Double]#arg[Double, Double]#to[GEOMETRY, GEOMETRY]) =
      srid match {
        case Some(s) => om.column(GeomLibrary.MakeEnvelope, xmin.toNode, ymin.toNode, xmax.toNode, ymax.toNode, LiteralNode(s))
        case None   =>  om.column(GeomLibrary.MakeEnvelope, xmin.toNode, ymin.toNode, xmax.toNode, ymax.toNode)
      }
    def makePoint[P1, P2, R](x: Column[P1], y: Column[P2], z: Option[Double] = None, m: Option[Double] = None)(
      implicit tm: JdbcType[GEOMETRY], tm1: JdbcType[POINT], om: OptionMapperDSL.arg[Double, P1]#arg[Double, P2]#to[GEOMETRY, R]) =
      (z, m) match {
        case (Some(z), Some(m)) => om.column(GeomLibrary.MakePoint, x.toNode, y.toNode, LiteralNode(z), LiteralNode(m))
        case (Some(z), None) => om.column(GeomLibrary.MakePoint, x.toNode, y.toNode, LiteralNode(z))
        case (None, Some(m)) => om.column(GeomLibrary.MakePointM, x.toNode, y.toNode, LiteralNode(m))
        case (None, None) => om.column(GeomLibrary.MakePoint, x.toNode, y.toNode)
      }
    def makeLine[G1 <: GEOMETRY, P1, G2 <: GEOMETRY, P2, R](point1: Column[P1], point2: Column[P2])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[G1, P1]#arg[G2, P2]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.MakeLine, point1.toNode, point2.toNode)
      }
    def makePolygon[G <: GEOMETRY, P, R](linestring: Column[P])(
      implicit tm: JdbcType[GEOMETRY], om: OptionMapperDSL.arg[G, P]#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.MakePolygon, linestring.toNode)
      }
  }
  
  //////////////////////////////////////////////////////////////////////////////////

  object GeomLibrary {
    /** Geometry Operators */
    val BoxIntersects = new SqlOperator("&&")
    val BoxIntersects3D = new SqlOperator("&&&")
    val BoxContains = new SqlOperator("~")
    val BoxContainedBy = new SqlOperator("@")
//    val BoxEquals = new SqlOperator("=")  // it's not necessary
    val PointDistance = new SqlOperator("<->")
    val BoxDistance = new SqlOperator("<#>")

    val BoxLooseLeft = new SqlOperator("&<")
    val BoxStrictLeft = new SqlOperator("<<")
    val BoxLooseBelow = new SqlOperator("&<|")
    val BoxStrictBelow = new SqlOperator("<<|")
    val BoxLooseRight = new SqlOperator("&>")
    val BoxStrictRight = new SqlOperator(">>")
    val BoxLooseAbove = new SqlOperator("|&>")
    val BoxStrictAbove = new SqlOperator("|>>")

    /** Geometry Constructors */
    val GeomFromText = new SqlFunction("ST_GeomFromText")
    val GeomFromWKB = new SqlFunction("ST_GeomFromWKB")
    val GeomFromEWKT = new SqlFunction("ST_GeomFromEWKT")
    val GeomFromEWKB = new SqlFunction("ST_GeomFromEWKB")
    val GeomFromGML = new SqlFunction("ST_GeomFromGML")
    val GeomFromKML = new SqlFunction("ST_GeomFromKML")
    val GeomFromGeoJSON = new SqlFunction("ST_GeomFromGeoJSON")
    val MakeBox = new SqlFunction("ST_MakeBox2D")
    val MakeBox3D = new SqlFunction("ST_3DMakeBox")
    val MakeEnvelope = new SqlFunction("ST_MakeEnvelope")
    val MakePoint = new SqlFunction("ST_MakePoint")
    val MakePointM = new SqlFunction("ST_MakePointM")
    val MakeLine = new SqlFunction("ST_MakeLine")
    val MakePolygon = new SqlFunction("ST_MakePolygon")

    /** Geometry Accessors */
    val GeometryType = new SqlFunction("ST_GeometryType")
    val SRID = new SqlFunction("ST_SRID")
    val IsValid = new SqlFunction("ST_IsValid")
    val IsClosed = new SqlFunction("ST_IsClosed")
    val IsCollection = new SqlFunction("ST_IsCollection")
    val IsEmpty = new SqlFunction("ST_IsEmpty")
    val IsRing = new SqlFunction("ST_IsRing")
    val IsSimple = new SqlFunction("ST_IsSimple")
    val Area = new SqlFunction("ST_Area")
    val Boundary = new SqlFunction("ST_Boundary")
    val Dimension = new SqlFunction("ST_Dimension")
    val CoordDim = new SqlFunction("ST_CoordDim")
    val NDims = new SqlFunction("ST_NDims")
    val NPoints = new SqlFunction("ST_NPoints")
    val NRings = new SqlFunction("ST_NRings")
    val X = new SqlFunction("ST_X")
    val Y = new SqlFunction("ST_Y")
    val Z = new SqlFunction("ST_Z")
    val XMax = new SqlFunction("ST_XMax")
    val XMin = new SqlFunction("ST_XMin")
    val YMax = new SqlFunction("ST_YMax")
    val YMin = new SqlFunction("ST_YMin")
    val ZMax = new SqlFunction("ST_ZMax")
    val ZMin = new SqlFunction("ST_ZMin")
    val Zmflag = new SqlFunction("ST_Zmflag")
    val Box3D = new SqlFunction("Box3D")

    /** Geometry Outputs */
    val AsBinary = new SqlFunction("ST_AsBinary")
    val AsText = new SqlFunction("ST_AsText")
    val AsLatLonText = new SqlFunction("ST_AsLatLonText")
    val AsEWKB = new SqlFunction("ST_AsEWKB")
    val AsEWKT = new SqlFunction("ST_AsEWKT")
    val AsHEXEWKB = new SqlFunction("ST_AsHEXEWKB")
    val AsGeoJSON = new SqlFunction("ST_AsGeoJSON")
    val AsGeoHash = new SqlFunction("ST_GeoHash")
    val AsGML = new SqlFunction("ST_AsGML")
    val AsKML = new SqlFunction("ST_AsKML")
    val AsSVG = new SqlFunction("ST_AsSVG")
    val AsX3D = new SqlFunction("ST_AsX3D")

    /** Spatial Relationships */
    val HasArc = new SqlFunction("ST_HasArc")
    val Equals = new SqlFunction("ST_Equals")
    val OrderingEquals = new SqlFunction("ST_OrderingEquals")
    val Overlaps = new SqlFunction("ST_Overlaps")
    val Intersects = new SqlFunction("ST_Intersects")
    val Crosses = new SqlFunction("ST_Crosses")
    val Disjoint = new SqlFunction("ST_Disjoint")
    val Contains = new SqlFunction("ST_Contains")
    val ContainsProperly = new SqlFunction("ST_ContainsProperly")
    val Within = new SqlFunction("ST_Within")
    val DWithin = new SqlFunction("ST_DWithin")
    val DFullyWithin = new SqlFunction("ST_DFullyWithin")
    val Touches = new SqlFunction("ST_Touches")
    val Relate = new SqlFunction("ST_Relate")

    /** Spatial Measurements */
    val Azimuth = new SqlFunction("ST_Azimuth")
    val Centroid = new SqlFunction("ST_Centroid")
    val ClosestPoint = new SqlFunction("ST_ClosestPoint")
    val PointOnSurface = new SqlFunction("ST_PointOnSurface")
    val Project = new SqlFunction("ST_Project")
    val Length = new SqlFunction("ST_Length")
    val Length3D = new SqlFunction("ST_3DLength")
    val Perimeter = new SqlFunction("ST_Perimeter")
    val Distance = new SqlFunction("ST_Distance")
    val DistanceSphere = new SqlFunction("ST_Distance_Sphere")
    val MaxDistance = new SqlFunction("ST_MaxDistance")
    val HausdorffDistance = new SqlFunction("ST_HausdorffDistance")
    val LongestLine = new SqlFunction("ST_LongestLine")
    val ShortestLine = new SqlFunction("ST_ShortestLine")

    /** Geometry Processing */
    val SetSRID = new SqlFunction("ST_SetSRID")
    val Transform = new SqlFunction("ST_Transform")
    val Simplify = new SqlFunction("ST_Simplify")
    val RemoveRepeatedPoints = new SqlFunction("ST_RemoveRepeatedPoints")
    val SimplifyPreserveTopology = new SqlFunction("ST_SimplifyPreserveTopology")
    val Difference = new SqlFunction("ST_Difference")
    val SymDifference = new SqlFunction("ST_SymDifference")
    val Intersection = new SqlFunction("ST_Intersection")
    val SharedPaths = new SqlFunction("ST_SharedPaths")
    val Split = new SqlFunction("ST_Split")
    val MinBoundingCircle = new SqlFunction("ST_MinimumBoundingCircle")

    val Buffer = new SqlFunction("ST_Buffer")
    val Multi = new SqlFunction("ST_Multi")
    val LineMerge = new SqlFunction("ST_LineMerge")
    val CollectionExtract = new SqlFunction("ST_CollectionExtract")
    val CollectionHomogenize = new SqlFunction("ST_CollectionHomogenize")
    val AddPoint = new SqlFunction("ST_AddPoint")
    val SetPoint = new SqlFunction("ST_SetPoint")
    val RemovePoint = new SqlFunction("ST_RemovePoint")
    val Reverse = new SqlFunction("ST_Reverse")
    val Scale = new SqlFunction("ST_Scale")
    val Segmentize = new SqlFunction("ST_Segmentize")
    val Snap = new SqlFunction("ST_Snap")
    val Translate = new SqlFunction("ST_Translate")
  }

  /** Extension methods for postgis Columns */
  class GeometryColumnExtensionMethods[G1 <: GEOMETRY, P1]
    (val c: Column[P1])
    (implicit 
      tm: JdbcType[GEOMETRY], 
      tm1: JdbcType[POINT], 
      tm2: JdbcType[LINESTRING], 
      tm3: JdbcType[POLYGON], 
      tm4: JdbcType[GEOMETRYCOLLECTION]
    ) extends ExtensionMethods[G1, P1] {

    /** Geometry Operators */
    def @&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxIntersects, n, geom.toNode)
      }
    def @&&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxIntersects3D, n, geom.toNode)
      }
    def @>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxContains, n, geom.toNode)
      }
    def <@[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxContainedBy, n, geom.toNode)
      }
    def <->[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
        om.column(GeomLibrary.PointDistance, n, geom.toNode)
      }
    def <#>[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
        om.column(GeomLibrary.BoxDistance, n, geom.toNode)
      }

    def &<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxLooseLeft, n, geom.toNode)
      }
    def <<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxStrictLeft, n, geom.toNode)
      }
    def &<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxLooseBelow, n, geom.toNode)
      }
    def <<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxStrictBelow, n, geom.toNode)
      }
    def &>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxLooseRight, n, geom.toNode)
      }
    def >>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxStrictRight, n, geom.toNode)
      }
    def |&>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxLooseAbove, n, geom.toNode)
      }
    def |>>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.BoxStrictAbove, n, geom.toNode)
      }

    /** Geometry Accessors */
    def geomType[R](implicit om: o#to[String, R]) = {
        om.column(GeomLibrary.GeometryType, n)
      }
    def srid[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.SRID, n)
      }
    def isValid[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsValid, n)
      }
    def isClosed[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsClosed, n)
      }
    def isCollection[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsCollection, n)
      }
    def isEmpty[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsEmpty, n)
      }
    def isRing[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsRing, n)
      }
    def isSimple[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.IsSimple, n)
      }
    def hasArc[R](implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.HasArc, n)
      }
    def area[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Area, n)
      }
    def boundary[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Boundary, n)
      }
    def dimension[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.Dimension, n)
      }
    def coordDim[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.CoordDim, n)
      }
    def nDims[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.NDims, n)
      }
    def nPoints[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.NPoints, n)
      }
    def nRings[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.NRings, n)
      }
    def x[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.X, n)
      }
    def y[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Y, n)
      }
    def z[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Z, n)
      }
    def xmin[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.XMin, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def xmax[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.XMax, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def ymin[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.YMin, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def ymax[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.YMax, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def zmin[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.ZMin, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def zmax[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.ZMax, GeomLibrary.Box3D.column[GEOMETRY](n).toNode)
      }
    def zmflag[R](implicit om: o#to[Int, R]) = {
        om.column(GeomLibrary.Zmflag, n)
      }

    /** Geometry Outputs */
    def asBinary[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om.column(GeomLibrary.AsBinary, n, LiteralNode(endian))
        case None   => om.column(GeomLibrary.AsBinary, n)
      }
    def asText[R](implicit om: o#to[String, R]) = {
        om.column(GeomLibrary.AsText, n)
      }
    def asLatLonText[R](format: Option[String] = None)(implicit om: o#to[String, R]) =
      format match {
        case Some(fmt) => om.column(GeomLibrary.AsLatLonText, n, LiteralNode(fmt))
        case None   => om.column(GeomLibrary.AsLatLonText, n)
      }
    def asEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om.column(GeomLibrary.AsEWKB, n, LiteralNode(endian))
        case None   => om.column(GeomLibrary.AsEWKB, n)
      }
    def asEWKT[R](implicit om: o#to[String, R]) = {
        om.column(GeomLibrary.AsEWKT, n)
      }
    def asHEXEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[String, R]) =
      NDRorXDR match {
        case Some(endian) => om.column(GeomLibrary.AsHEXEWKB, n, LiteralNode(endian))
        case None   => om.column(GeomLibrary.AsHEXEWKB, n)
      }
    def asGeoJSON[R](maxDigits: Column[Int] = LiteralColumn(15), options: Column[Int] = LiteralColumn(0),
                     geoJsonVer: Option[Int] = None)(implicit om: o#to[String, R]) =
      geoJsonVer match {
        case Some(ver) => om.column(GeomLibrary.AsGeoJSON, LiteralNode(ver), n, maxDigits.toNode, options.toNode)
        case None   => om.column(GeomLibrary.AsGeoJSON, n, maxDigits.toNode, options.toNode)
      }
    def asGeoHash[R](maxChars: Option[Int] = None)(implicit om: o#to[String, R]) =
      maxChars match {
        case Some(charNum) => om.column(GeomLibrary.AsHEXEWKB, n, LiteralNode(charNum))
        case None   => om.column(GeomLibrary.AsHEXEWKB, n)
      }
    def asGML[R](maxDigits: Column[Int] = LiteralColumn(15), options: Column[Int] = LiteralColumn(0),
                 version: Option[Int] = None,  nPrefix: Option[String] = None)(implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om.column(GeomLibrary.AsGML, LiteralNode(ver), n, maxDigits.toNode, options.toNode, LiteralNode(prefix))
        case (Some(ver), None) => om.column(GeomLibrary.AsGML, LiteralNode(ver), n, maxDigits.toNode, options.toNode)
        case (_, _)   => om.column(GeomLibrary.AsGML, n, maxDigits.toNode, options.toNode)
      }
    def asKML[R](maxDigits: Column[Int] = LiteralColumn(15), version: Option[Int] = None, nPrefix: Option[String] = None)(
      implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om.column(GeomLibrary.AsKML, LiteralNode(ver), n, maxDigits.toNode, LiteralNode(prefix))
        case (Some(ver), None) => om.column(GeomLibrary.AsKML, LiteralNode(ver), n, maxDigits.toNode)
        case (_, _)   => om.column(GeomLibrary.AsKML, n, maxDigits.toNode)
      }
    def asSVG[R](rel: Column[Int] = LiteralColumn(0), maxDigits: Column[Int] = LiteralColumn(15))(implicit om: o#to[String, R]) = {
        om.column(GeomLibrary.AsSVG, n, rel.toNode, maxDigits.toNode)
      }
    def asX3D[R](maxDigits: Column[Int] = LiteralColumn(15), options: Column[Int] = LiteralColumn(0))(implicit om: o#to[String, R]) = {
        om.column(GeomLibrary.AsX3D, n, maxDigits.toNode, options.toNode)
      }

    /** Spatial Relationships */
    def gEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Equals, n, geom.toNode)
      }
    def orderingEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.OrderingEquals, n, geom.toNode)
      }
    def overlaps[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Overlaps, n, geom.toNode)
      }
    def intersects[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Intersects, n, geom.toNode)
      }
    def crosses[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Crosses, n, geom.toNode)
      }
    def disjoint[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Disjoint, n, geom.toNode)
      }
    def contains[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Contains, n, geom.toNode)
      }
    def containsProperly[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.ContainsProperly, n, geom.toNode)
      }
    def within[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Within, n, geom.toNode)
      }
    def dWithin[P2, R](geom: Column[P2], distance: Column[Double], useSpheroid: Option[Boolean] = None)(
      implicit om: o#to[Boolean, R]) = useSpheroid match {
        case Some(_) => om.column(GeomLibrary.DWithin, n, geom.toNode, distance.toNode, LiteralNode(useSpheroid.get))
        case _    =>    om.column(GeomLibrary.DWithin, n, geom.toNode, distance.toNode)
      }
    def dFullyWithin[P2, R](geom: Column[P2], distance: Column[Double])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.DFullyWithin, n, geom.toNode, distance.toNode)
      }
    def touches[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Touches, n, geom.toNode)
      }
    def relate[P2, R](geom: Column[P2], matrixPattern: Column[String])(implicit om: o#to[Boolean, R]) = {
        om.column(GeomLibrary.Relate, n, geom.toNode, matrixPattern.toNode)
      }
    def relatePattern[P2, R](geom: Column[P2], boundaryNodeRule: Option[Int] = None)(implicit om: o#to[String, R]) =
      boundaryNodeRule match {
        case Some(rule) => om.column(GeomLibrary.Relate, n, geom.toNode, LiteralNode(rule))
        case None    => om.column(GeomLibrary.Relate, n, geom.toNode)
      }

    /** Spatial Measurements */
    def azimuth[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Azimuth, n, geom.toNode)
      }
    def centroid[R](implicit om: o#to[POINT, R]) = {
        om.column(GeomLibrary.Centroid, n)
      }
    def closestPoint[P2, R](geom: Column[P2])(implicit om: o#to[POINT, R]) = {
        om.column(GeomLibrary.ClosestPoint, n, geom.toNode)
      }
    def pointOnSurface[R](implicit om: o#to[POINT, R]) = {
        om.column(GeomLibrary.PointOnSurface, n)
      }
    def project[R](distance: Column[Float], azimuth: Column[Float])(implicit om: o#to[POINT, R]) = {
        om.column(GeomLibrary.Project, n, distance.toNode, azimuth.toNode)
      }
    def length[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Length, n)
      }
    def length3d[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Length3D, n)
      }
    def perimeter[R](implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Perimeter, n)
      }
    def distance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.Distance, n, geom.toNode)
      }
    def distanceSphere[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.DistanceSphere, n, geom.toNode)
      }
    def maxDistance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om.column(GeomLibrary.MaxDistance, n, geom.toNode)
      }
    def hausdorffDistance[P2, R](geom: Column[P2], densifyFrac: Option[Float] = None)(implicit om: o#to[Float, R]) =
      densifyFrac match {
        case Some(denFrac) => om.column(GeomLibrary.HausdorffDistance, n, geom.toNode, LiteralNode(denFrac))
        case None   => om.column(GeomLibrary.HausdorffDistance, n, geom.toNode)
      }
    def longestLine[P2, R](geom: Column[P2])(implicit om: o#to[LINESTRING, R]) = {
        om.column(GeomLibrary.LongestLine, n, geom.toNode)
      }
    def shortestLine[P2, R](geom: Column[P2])(implicit om: o#to[LINESTRING, R]) = {
        om.column(GeomLibrary.ShortestLine, n, geom.toNode)
      }

    /** Geometry Processing */
    def setSRID[R](srid: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.SetSRID, n, srid.toNode)
      }
    def transform[R](srid: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Transform, n, srid.toNode)
      }
    def simplify[R](tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Simplify, n, tolerance.toNode)
      }
    def removeRepeatedPoints[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.RemoveRepeatedPoints, n)
      }
    def simplifyPreserveTopology[R](tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.SimplifyPreserveTopology, n, tolerance.toNode)
      }
    def difference[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Difference, n, geom.toNode)
      }
    def symDifference[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.SymDifference, n, geom.toNode)
      }
    def intersection[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Intersection, n, geom.toNode)
      }
    def sharedPaths[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.SharedPaths, n, geom.toNode)
      }
    def split[P2, R](blade: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Split, n, blade.toNode)
      }
    def minBoundingCircle[R](segNumPerQtrCircle: Column[Int] = LiteralColumn(48))(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.MinBoundingCircle, n, segNumPerQtrCircle.toNode)
      }

    def buffer[R](radius: Column[Float], bufferStyles: Option[String] = None)(implicit om: o#to[GEOMETRY, R]) =
      bufferStyles match {
        case Some(styles) => om.column(GeomLibrary.Buffer, n, radius.toNode, LiteralNode(styles))
        case None   =>  om.column(GeomLibrary.Buffer, n, radius.toNode)
      }
    def multi[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Multi, n)
      }
    def lineMerge[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.LineMerge, n)
      }
    def collectionExtract[R](tpe: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.CollectionExtract, n, tpe.toNode)
      }
    def collectionHomogenize[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.CollectionHomogenize, n)
      }
    def addPoint[P2, R](point: Column[P2], position: Option[Int] = None)(implicit om: o#to[GEOMETRY, R]) =
      position match {
        case Some(pos) => om.column(GeomLibrary.AddPoint, n, point.toNode, LiteralNode(pos))
        case None   =>  om.column(GeomLibrary.AddPoint, n, point.toNode)
      }
    def setPoint[P2, R](point: Column[P2], position: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.SetPoint, n, position.toNode, point.toNode)
      }
    def removePoint[R](offset: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.RemovePoint, n, offset.toNode)
      }
    def reverse[R](implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Reverse, n)
      }
    def scale[R](xFactor: Column[Float], yFactor: Column[Float], zFactor: Option[Float] = None)(implicit om: o#to[GEOMETRY, R]) =
      zFactor match {
        case Some(zFac) => om.column(GeomLibrary.Scale, n, xFactor.toNode, yFactor.toNode, LiteralNode(zFac))
        case None   =>  om.column(GeomLibrary.Scale, n, xFactor.toNode, yFactor.toNode)
      }
    def segmentize[R](maxLength: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Segmentize, n, maxLength.toNode)
      }
    def snap[P2, R](reference: Column[P2], tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om.column(GeomLibrary.Snap, n, reference.toNode, tolerance.toNode)
      }
    def translate[R](deltaX: Column[Float], deltaY: Column[Float], deltaZ: Option[Float] = None)(implicit om: o#to[GEOMETRY, R]) =
      deltaZ match {
        case Some(deltaZ) => om.column(GeomLibrary.Translate, n, deltaX.toNode, deltaY.toNode, LiteralNode(deltaZ))
        case None   =>  om.column(GeomLibrary.Translate, n, deltaX.toNode, deltaY.toNode)
      }
  }
}
