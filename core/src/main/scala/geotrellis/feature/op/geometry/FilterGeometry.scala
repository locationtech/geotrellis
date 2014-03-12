/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

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