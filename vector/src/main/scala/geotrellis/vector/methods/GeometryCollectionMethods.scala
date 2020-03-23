/*
 * Copyright 2020 Azavea
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
