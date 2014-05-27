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

case class GTKDMetadata(version: Int, keyRevision: Int,
  minorRevision: Int, numberOfKeys: Int)

class GTKeyEntryData

case class GTKEMetadata(keyID: Int, tiffTagLocation: Int, count: Int,
  valueOffset: Int)

case class GTKeyEntry(metadata: GTKEMetadata, data: GTKeyEntryData)

case class GTKEModelType(modelType: Int) extends GTKeyEntryData

case class GTKERasterType(rasterType: Int) extends GTKeyEntryData

case class GTKECitation(citations: Array[String]) extends GTKeyEntryData

case class GTKEGeographicType(geographicType: Int) extends GTKeyEntryData

case class GTKEGeogCitation() extends GTKeyEntryData

case class GTKEGeogGeodeticDatum() extends GTKeyEntryData

case class GTKEGeogPrimeMeridian() extends GTKeyEntryData

case class GTKEGeogLinearUnits(linearUnits: Int) extends GTKeyEntryData

case class GTKEGeogLinearUnitSize() extends GTKeyEntryData

case class GTKEGeogAngularUnits(angularUnits: Int) extends GTKeyEntryData

case class GTKEGeogAngularUnitSize() extends GTKeyEntryData

case class GTKEGeogEllipsoid() extends GTKeyEntryData

case class GTKEGeogSemiMajorAxis() extends GTKeyEntryData

case class GTKEGeogSemiMinorAxis() extends GTKeyEntryData

case class GTKEGeogInvFlattening() extends GTKeyEntryData

case class GTKEGeogAzimuthUnits() extends GTKeyEntryData

case class GTKEGeogPrimeMeridianLong() extends GTKeyEntryData

case class GTKEProjectedCSType(projectedCSType: Int) extends GTKeyEntryData

case class GTKEPCSCitation() extends GTKeyEntryData

case class GTKEProjection(projection: Int) extends GTKeyEntryData

case class GTKEProjCoordTrans(coordTrans: Int) extends GTKeyEntryData

case class GTKEProjLinearUnits(linearUnits: Int) extends GTKeyEntryData

case class GTKEProjLinearUnitSize() extends GTKeyEntryData

case class GTKEProjStdParallel1(stdParallel1: Double) extends GTKeyEntryData

case class GTKEProjStdParallel2(stdParallel2: Double) extends GTKeyEntryData

case class GTKEProjNatOriginLong() extends GTKeyEntryData

case class GTKEProjNatOriginLat(natOriginLat: Double) extends GTKeyEntryData

case class GTKEProjFalseEasting(falseEasting: Double) extends GTKeyEntryData

case class GTKEProjFalseNorthing(falseNorthing: Double) extends GTKeyEntryData

case class GTKEProjFalseOriginLong() extends GTKeyEntryData

case class GTKEProjFalseOriginLat() extends GTKeyEntryData

case class GTKEProjFalseOriginEasting() extends GTKeyEntryData

case class GTKEProjFalseOriginNorthing() extends GTKeyEntryData

case class GTKEProjCenterLong(centerLong: Double) extends GTKeyEntryData

case class GTKEProjCenterLat() extends GTKeyEntryData

case class GTKEProjCenterEasting() extends GTKeyEntryData

case class GTKEProjCenterNorthing() extends GTKeyEntryData

case class GTKEProjScaleAtNatOrigin() extends GTKeyEntryData

case class GTKEProjScaleAtCenter() extends GTKeyEntryData

case class GTKEProjAzimuthAngle() extends GTKeyEntryData

case class GTKEProjStraightVertPoleLong() extends GTKeyEntryData

case class GTKEVerticalCSType() extends GTKeyEntryData

case class GTKEVerticalCitation() extends GTKeyEntryData

case class GTKEVerticalDatum() extends GTKeyEntryData

case class GTKEVerticalUnits() extends GTKeyEntryData
