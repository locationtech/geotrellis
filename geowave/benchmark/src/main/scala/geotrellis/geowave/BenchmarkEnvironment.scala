package geotrellis.geowave

import cats.effect.{ContextShift, IO, Timer}
import scala.util.Properties

trait BenchmarkEnvironment {
  val kafka: String     = Properties.envOrElse("KAFKA_HOST", "localhost:9092")
  val cassandra: String = Properties.envOrElse("CASSANDRA_HOST", "localhost")

  implicit val contextShift: ContextShift[IO] = BlockingThreadPool.contextShiftIO
  implicit val timer: Timer[IO]               = BlockingThreadPool.timerIO
}
