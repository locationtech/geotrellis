/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.store.accumulo

import geotrellis.store._
import geotrellis.store.accumulo._
import geotrellis.spark.store._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.scalatest.BeforeAndAfterAll
import org.apache.accumulo.minicluster.MiniAccumuloCluster

import com.google.common.io.Files

class AccumuloAttributeStoreSpec extends AttributeStoreSpec with BeforeAndAfterAll {
  var miniAccumuloCluster: MiniAccumuloCluster = _

  override def beforeAll(): Unit = {
    val tempDir = Files.createTempDir
    tempDir.deleteOnExit()
    miniAccumuloCluster = new MiniAccumuloCluster(tempDir, "")
    miniAccumuloCluster.start()
  }

  override def afterAll(): Unit = miniAccumuloCluster.stop()

  lazy val accumulo = AccumuloInstance(
    instanceName = miniAccumuloCluster.getInstanceName,
    zookeeper = miniAccumuloCluster.getZooKeepers,
    user = "root",
    token = new PasswordToken("")
  )

  lazy val attributeStore = new AccumuloAttributeStore(accumulo.connector, "attributes")

  it("should read the Layer's Header as a LayerHeader and then an as an AccumuloLayerHeader") {
    val header = AccumuloLayerHeader("SpatialKey", "Tile", attributeStore.attributeTable)

    attributeStore.write[AccumuloLayerHeader](LayerId("test1", 1), "header", header)

    attributeStore.readHeader[LayerHeader](LayerId("test1", 1))
    attributeStore.readHeader[AccumuloLayerHeader](LayerId("test1", 1))
  }
}
