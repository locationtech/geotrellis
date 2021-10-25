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

package geotrellis.store

import geotrellis.spark.testkit.TestEnvironment

import org.scalatest.Suite

import java.io.File

trait CatalogTestEnvironment extends TestEnvironment { self: Suite =>
  override def beforeAll() = {
    val multibandDir = new File(TestCatalog.multibandOutputPath)
    // length >= 2 means that in the directory there are at least two folders - attributes and at least one layer folder
    if (!(multibandDir.exists && multibandDir.list().length >= 2)) TestCatalog.createMultiband
    else println(s"Test multi-band catalog exists at: $multibandDir")
    val singlebandDir = new File(TestCatalog.singlebandOutputPath)
    if (!(singlebandDir.exists && singlebandDir.list().length >= 2)) TestCatalog.createSingleband
    else println(s"Test single-band catalog exists at: $singlebandDir")
  }
}
