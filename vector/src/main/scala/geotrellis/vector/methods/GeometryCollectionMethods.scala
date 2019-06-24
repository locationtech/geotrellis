package geotrellis.vector.methods

import geotrellis.vector._
import geotrellis.util.MethodExtensions
import scala.reflect._
import spire.syntax.cfor._

trait ExtraGeometryCollectionMethods extends MethodExtensions[GeometryCollection] {
  def getAll[G <: Geometry : ClassTag]: Seq[G] = {
    val lb = scala.collection.mutable.ListBuffer.empty[G]
    cfor(0)(_ < self.getNumGeometries, _ + 1){ i =>
      if (classTag[G].runtimeClass.isInstance(self.getGeometryN(i)))
        lb += self.getGeometryN(i).asInstanceOf[G]
    }
    lb.toSeq
  }

  def geometries: Seq[Geometry] = {
    val lb = scala.collection.mutable.ListBuffer.empty[Geometry]
    cfor(0)(_ < self.getNumGeometries, _ + 1){ i =>
      lb += self.getGeometryN(i)
    }
    lb.toSeq
  }

  def normalized(): GeometryCollection = {
    val res = self.copy.asInstanceOf[GeometryCollection]
    res.normalize
    res
  }
}
