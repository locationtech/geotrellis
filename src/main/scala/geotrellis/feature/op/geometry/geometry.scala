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
}


