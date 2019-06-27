package geotrellis.util

import geotrellis.util.conf.BlockingThreadPoolConfig

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext

object BlockingThreadPool extends Serializable {
  @transient lazy val pool: ExecutorService = Executors.newFixedThreadPool(BlockingThreadPoolConfig.conf.threads)
  @transient lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutor(pool)
}
