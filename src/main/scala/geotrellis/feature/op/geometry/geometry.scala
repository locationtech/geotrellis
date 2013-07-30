package geotrellis.feature.op

import geotrellis._
import geotrellis.feature._
import geotrellis.{ op => liftOp }
import com.vividsolutions.jts.{ geom => jts }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

package object geometry {

  val GetExtent = liftOp { (a: Raster) => a.rasterExtent.extent }
  val AsFeature = liftOp { (e: Extent) => e.asFeature(None) }


  /**
   * Given list of geometries, inspect the underlying geometry type,
   * selecting only geometries that match G
   */
  case class FilterGeometry[G <: jts.Geometry,D](s: Op[List[Geometry[D]]])(
    implicit t: TypeTag[G])
      extends Op1(s)( s => {
        val typeName = t.tpe.typeSymbol.name.toString()
        val result = s.filter(_.geom.getGeometryType == typeName)
        Result( result )
      })


}
