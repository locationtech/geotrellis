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

object GTFieldMetadata {

  def apply(tag: Int, fieldType: Int, length: Int, offset: Int) =
    new GTFieldMetadata(tag, fieldType, length, offset)

}

class GTFieldMetadata(val tag: Int, val fieldType: Int,
  val length: Int, val offset: Int) { }
