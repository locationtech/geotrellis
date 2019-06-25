package geotrellis.util

import geotrellis.util.conf.BlockingThreadPoolConfig
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object BlockingThreadPool extends Serializable {
  @transient lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(BlockingThreadPoolConfig.conf.threads))
}
