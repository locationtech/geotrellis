/*
 * Copyright 2019 Azavea
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

package geotrellis.raster

import java.net.URI

/**
 * Represents the path to data that is to be read.
 */
trait SourcePath extends SourceName {

  /**
    * The given path to the data. This can be formatted in a number of different
    * ways depending on which [[RasterSource]] is to be used. For more information
    * on the different ways of formatting this string, see the docs on the
    * DataPath for that given soure.
    */
  def value: String

  override def toString: String = value

  def toURI: URI = new URI(value)
}
