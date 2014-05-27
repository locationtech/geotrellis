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

case class GTFieldMetadata(tag: Int, fieldType: Int,
  length: Int, offset: Int)

class GTFieldData { }

case class GeoTiffField(metadata: GTFieldMetadata, data: GTFieldData)

case class GTFieldImageWidth(width: Int) extends GTFieldData

case class GTFieldImageHeight(height: Int) extends GTFieldData

case class GTFieldBitsPerSample(bitePerSample: Int) extends GTFieldData

case class GTFieldCompression(compression: Int) extends GTFieldData

case class GTFieldPhotometricInterpretation(photometricInterp: Int)
    extends GTFieldData

case class GTFieldStripOffsets(stripOffsets: Array[Int]) extends GTFieldData

case class GTFieldSamplesPerPixel(samplesPerPixel: Int) extends GTFieldData

case class GTFieldRowsPerStrip(rowsPerStrip: Int) extends GTFieldData

case class GTFieldStripByteCounts(stripByteCounts: Array[Int])
    extends GTFieldData

case class GTFieldXResolution(xResolution: Double) extends GTFieldData

case class GTFieldYResolution(yResolution: Double) extends GTFieldData

case class GTFieldResolutionUnit(resolutionUnit: Int) extends GTFieldData

case class GTFieldColorMap(colorMap: Array[Int]) extends GTFieldData

case class GTFieldPlanarConfiguration(planarConfiguration: Int)
    extends GTFieldData

case class GTFieldModelPixelScale(scaleX: Double, scaleY: Double,
  scaleZ: Double) extends GTFieldData

case class GTModelTiePoint(i: Double, j: Double, k: Double,
  x: Double, y: Double, z:Double)

case class GTFieldModelTiePoints(points: Array[GTModelTiePoint])
    extends GTFieldData

case class GTFieldGeoKeyDirectory(metadata: GTKDMetadata,
  keyEntries: Array[GTKeyEntry]) extends GTFieldData

case class GTFieldGeoDoubleParams(values: Array[Double]) extends GTFieldData

case class GTFieldGeoAsciiParams(value: String) extends GTFieldData
