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

package geotrellis.layers.avro.codecs

import geotrellis.raster.{ConstantTile, Tile}
import geotrellis.layers.avro.{AvroRecordCodec, AvroUnionCodec}

object Implicits extends Implicits

trait Implicits
    extends TileCodecs
    with ConstantTileCodecs
    with TileFeatureCodec
    with CRSCodec
    with ExtentCodec
    with ProjectedExtentCodec
    with TemporalProjectedExtentCodec
    with KeyCodecs {
  implicit def constantTileUnionCodec = new AvroUnionCodec[ConstantTile](
    byteConstantTileCodec,
    floatConstantTileCodec,
    doubleConstantTileCodec,
    shortConstantTileCodec,
    intConstantTileCodec,
    bitConstantTileCodec,
    uByteConstantTileCodec,
    uShortConstantTileCodec
  )

  implicit def tileUnionCodec = new AvroUnionCodec[Tile](
    byteArrayTileCodec,
    floatArrayTileCodec,
    doubleArrayTileCodec,
    shortArrayTileCodec,
    intArrayTileCodec,
    bitArrayTileCodec,
    uByteArrayTileCodec,
    uShortArrayTileCodec,
    byteConstantTileCodec,
    floatConstantTileCodec,
    doubleConstantTileCodec,
    shortConstantTileCodec,
    intConstantTileCodec,
    bitConstantTileCodec,
    uByteConstantTileCodec,
    uShortConstantTileCodec
  )

  implicit def tupleCodec[A: AvroRecordCodec, B: AvroRecordCodec]: TupleCodec[A, B] = TupleCodec[A, B]
}
