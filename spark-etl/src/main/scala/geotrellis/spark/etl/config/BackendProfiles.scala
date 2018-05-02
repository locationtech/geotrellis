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

package geotrellis.spark.etl.config

import geotrellis.spark.io.accumulo.AccumuloInstance
import geotrellis.spark.io.cassandra.conf.CassandraConfig
import geotrellis.spark.io.hbase.HBaseInstance
import geotrellis.spark.io.cassandra.{BaseCassandraInstance}
import org.apache.accumulo.core.client.security.tokens.PasswordToken

sealed trait BackendProfile {
  val name: String
  def `type`: BackendType
}

case class HadoopProfile(name: String) extends BackendProfile { def `type` = HadoopType }
case class S3Profile(name: String, partitionsCount: Option[Int] = None, partitionsBytes: Option[Int] = None) extends BackendProfile { def `type` = S3Type }
case class CassandraProfile(name: String, hosts: String, user: String, password: String, cassandraConfig: CassandraConfig = CassandraConfig) extends BackendProfile {
  def `type` = CassandraType

  def getInstance = BaseCassandraInstance(
    hosts.split(","),
    user,
    password,
    cassandraConfig
  )
}
case class AccumuloProfile(name: String, instance: String, zookeepers: String, user: String, password: String, strategy: Option[String] = None, ingestPath: Option[String] = None) extends BackendProfile {
  def `type` = AccumuloType
  def token = new PasswordToken(password)

  def getInstance = AccumuloInstance(instance, zookeepers, user, token)
}
case class HBaseProfile(name: String, master: String, zookeepers: String) extends BackendProfile {
  def `type` = HBaseType

  def getInstance = HBaseInstance(zookeepers.split(","), master)
}
