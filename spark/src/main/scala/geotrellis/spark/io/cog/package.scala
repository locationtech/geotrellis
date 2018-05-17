/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io

import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent

import org.apache.spark.util.AccumulatorV2
import java.util

package object cog extends Implicits {
  val GTKey     = "GT_KEY"
  val Extension = "tiff"

  object COGAttributeStore {
    object Fields {
      val metadataBlob = "metadata"
      val metadata = "metadata"
      val header   = "header"
    }
  }

  implicit class withExtentMethods(extent: Extent) {
    def bufferByLayout(layout: LayoutDefinition): Extent =
      Extent(
        extent.xmin + layout.cellwidth / 2,
        extent.ymin + layout.cellheight / 2,
        extent.xmax - layout.cellwidth / 2,
        extent.ymax - layout.cellheight / 2
      )
  }
}
