/*
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
 */

package geotrellis.io.geotiffreader

class GTFieldData { }

case class GTFieldImageWidth(width: Int) extends GTFieldData

case class GTFieldImageHeight(height: Int) extends GTFieldData

case class GTFieldBitsPerSample(bitePerSample: Int) extends GTFieldData

case class GTFieldCompression(compressionType: Int) extends GTFieldData

case class GTFieldPhotometricInterpretation(interpretation: Int)
    extends GTFieldData

case class GTFieldStripOffsets(stripOffsets: Array[Int]) extends GTFieldData

case class GTFieldSamplesPerPixel(samplesPerPixel: Int) extends GTFieldData

case class GTFieldRowsPerStrip(rowsPerStrip: Int) extends GTFieldData

case class GTFieldStripByteCounts(stripByteCounts: Array[Int])
    extends GTFieldData

case class GTFieldPlanarConfiguration(planarConfiguration: Int)
    extends GTFieldData
