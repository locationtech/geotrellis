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

package geotrellis.spark.io.accumulo

import geotrellis.layers.accumulo._
import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.client.mapreduce.{AbstractInputFormat => AIF, AccumuloOutputFormat => AOF}
import org.apache.hadoop.mapreduce.Job

object MockAccumuloInstance { def apply(): MockAccumuloInstance = new MockAccumuloInstance }

class MockAccumuloInstance() extends AccumuloInstance {
  def instance: Instance = new MockInstance("fake")
  def user = "root"
  def token = new PasswordToken("")
  def connector = instance.getConnector(user, token)
  def instanceName = "fake"

  def setAccumuloConfig(job: Job): Unit = {
    AIF.setMockInstance(job, instanceName)
    AOF.setMockInstance(job, instanceName)
    AIF.setConnectorInfo(job, user, token)
    AOF.setConnectorInfo(job, user, token)
  }
}
