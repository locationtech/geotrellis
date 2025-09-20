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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags.TiffTags

trait Compressor extends Serializable {
  def compress(bytes: Array[Byte], segmentIndex: Int): Array[Byte]

  /** Returns the decompressor that can decompress the segments compressed by this compressor */
  def createDecompressor(): Decompressor

  def withPredictorEncoding(predictor: Predictor): Compressor =
    new Compressor {
      def wrapped: Compressor = Compressor.this

      def compress(bytes: Array[Byte], segmentIndex: Int): Array[Byte] =
        wrapped.compress(predictor.encode(bytes, segmentIndex), segmentIndex = segmentIndex)

      /** Returns the decompressor that can decompress the segments compressed by this compressor */
      def createDecompressor(): Decompressor = wrapped.createDecompressor().withPredictorDecoding(predictor)
    }

}
