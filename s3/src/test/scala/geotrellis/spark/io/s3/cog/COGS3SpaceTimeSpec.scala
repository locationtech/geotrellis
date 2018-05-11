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

package geotrellis.spark.io.s3.cog

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.cog._
import geotrellis.spark.io.s3.testkit._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog._

import org.scalatest._

class COGS3SpaceTimeSpec
  extends COGPersistenceSpec[SpaceTimeKey, Tile]
    with COGSpaceTimeKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGCoordinateSpaceTimeSpec
    with COGLayerUpdateSpaceTimeTileSpec {

  registerAfterAll { () =>
    MockS3Client.reset()
  }

  lazy val bucket = "mock-bucket"
  lazy val prefix = "catalog"

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client
  }

  lazy val reader = S3COGLayerReader(attributeStore)
  lazy val creader = S3COGCollectionLayerReader(attributeStore)
  lazy val writer = S3COGLayerWriter(attributeStore)
  // TODO: implement and test all layer functions
  // lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client }
  // lazy val copier = new S3LayerCopier(attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  // lazy val reindexer = GenericLayerReindexer[S3LayerHeader](attributeStore, reader, writer, deleter, copier)
  // lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = S3COGValueReader(attributeStore)
  lazy val sample =  CoordinateSpaceTime
}
