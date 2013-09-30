package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.{ geom => jts }

  /**
   * Given list of geometries, inspect the underlying geometry type,
   * selecting only geometries that match G
   */
/*
  case class FilterGeometry2[G <: jts.Geometry,D](s: Op[List[Geometry[D]]])(
    implicit t: TypeTag[G])
      extends Op1(s)( s => {
        val typeName = t.tpe.typeSymbol.name.toString()
        val result = s.filter(_.geom.getGeometryType == typeName)
        Result( result )
      })
*/