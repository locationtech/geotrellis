/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.rest.op.string

import geotrellis._

object ParseRasterExtent {
  def apply(bbox:Op[String], colsOp:Op[String], rowsOp:Op[String]) = {
    (ParseExtent(bbox), ParseInt(colsOp), ParseInt(rowsOp)).map { (e,c,r) =>
      RasterExtent(e,c,r)
    }
  }
}
