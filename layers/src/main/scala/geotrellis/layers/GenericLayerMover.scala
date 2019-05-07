/*
 * Copyright 2016 Azavea
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

package geotrellis.layers

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.json._
import geotrellis.util._

import spray.json._

import scala.reflect.ClassTag

class GenericLayerMover[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) extends LayerMover[ID] {
  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: ID, to: ID): Unit = {
    layerCopier.copy[K, V, M](from, to)
    layerDeleter.delete(from)
  }
}

object GenericLayerMover {
  def apply[ID](layerCopier: LayerCopier[ID], layerDeleter: LayerDeleter[ID]) =
    new GenericLayerMover(layerCopier, layerDeleter)
}
