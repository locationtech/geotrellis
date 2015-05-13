package geotrellis.spark.utils


import java.util.concurrent.Executors
import scala.concurrent.duration._
import scalaz._
import scalaz.concurrent._
import scalaz.stream._
import scalaz.stream.async._
import collection.JavaConversions._
import scala.util.Random

object TaskUtils extends App {
   implicit class TaskBackoff[A](task: Task[A]) {
    /**
     * Implement non-blocking Exponential Backoff on a Task.
     * @param  p  returns true for exceptions that trigger a backoff and retry
     */
    def retryEBO(p: (Throwable => Boolean) ): Task[A] = {
      def help(count: Int): Future[Throwable \/ A] = {        
        val base: Duration = 52.milliseconds
        val timeout = base * Random.nextInt(math.pow(2,count).toInt) // .extInt is [), implying -1
        task.get flatMap {
          case -\/(e) if p(e) =>
            help(count + 1) after timeout
          case x => Future.now(x)
        }
      }
      Task.async { help(0).runAsync }
    }
  }
}
