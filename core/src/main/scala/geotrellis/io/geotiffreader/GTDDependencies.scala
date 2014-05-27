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

trait GTDDependencies {

  def add(dependency: GTFieldData): GTDDependencies = this

  def add(metadata: GTFieldMetadata): GTDDependencies = this

}

case class GTDDEmpty() extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case doubleParams: GTFieldGeoDoubleParams => GTDDDouble(doubleParams)
    case asciiParams: GTFieldGeoAsciiParams => GTDDAscii(asciiParams)
  }

  override def add(metadata: GTFieldMetadata) = GTDDMetadata(metadata)

}

case class GTDDMetadata(metadata: GTFieldMetadata)
    extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case doubleParams: GTFieldGeoDoubleParams => GTDDMetadataDouble(metadata,
      doubleParams)
    case asciiParams: GTFieldGeoAsciiParams => GTDDMetadataAscii(metadata,
      asciiParams)
  }

}

case class GTDDDouble(doubleParams: GTFieldGeoDoubleParams)
    extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case asciiParams: GTFieldGeoAsciiParams => GTDDAsciiDouble(asciiParams,
      doubleParams)
  }

  override def add(metadata: GTFieldMetadata) = GTDDMetadataDouble(metadata,
    doubleParams)

}

case class GTDDAscii(asciiParams: GTFieldGeoAsciiParams)
    extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case doubleParams: GTFieldGeoDoubleParams => GTDDAsciiDouble(asciiParams,
      doubleParams)
  }

  override def add(metadata: GTFieldMetadata) = GTDDMetadataAscii(metadata,
    asciiParams)

}

case class GTDDMetadataDouble(metadata: GTFieldMetadata,
  doubleParams: GTFieldGeoDoubleParams) extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case asciiParams: GTFieldGeoAsciiParams => GTDDComplete(asciiParams,
      doubleParams, metadata)
  }

}

case class GTDDAsciiDouble(asciiParams: GTFieldGeoAsciiParams,
  doubleParams: GTFieldGeoDoubleParams) extends GTDDependencies {

  override def add(metadata: GTFieldMetadata) = GTDDComplete(asciiParams,
    doubleParams, metadata)

}

case class GTDDMetadataAscii(metadata: GTFieldMetadata,
  asciiParams: GTFieldGeoAsciiParams) extends GTDDependencies {

  override def add(dependency: GTFieldData) = dependency match {
    case doubleParams: GTFieldGeoDoubleParams => GTDDComplete(asciiParams,
      doubleParams, metadata)
  }

}

case class GTDDComplete(asciiParams: GTFieldGeoAsciiParams,
  doubleParams: GTFieldGeoDoubleParams, metadata: GTFieldMetadata)
    extends GTDDependencies
