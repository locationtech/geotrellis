package geotrellis.spark.io.accumulo

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
