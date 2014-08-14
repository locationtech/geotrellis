package geotrellis.spark.io.hadoop

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.hadoop.conf._
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce._
import org.apache.spark.rdd.RDD
import org.apache.spark._

class NewHadoopPartition(rddId: Int, val index: Int, @transient rawSplit: InputSplit with Writable)
  extends Partition {

  val serializableHadoopSplit = new SerializableWritable(rawSplit)

  override def hashCode(): Int = 41 * (41 + rddId) + index
}

/**
 * Extent this class in order to have an RDD that will choose which partitions to
 * de-serialize at all based on the RDD key. This will potentially greatly reduce the number
 * of spark Tasks spawned by using this RDD only to those nodes that have the data.
 *
 * @param sc The SparkContext to associate the RDD with.
 * @param inputFormatClass Storage format of the data to be read.
 * @param keyClass Class of the key associated with the inputFormatClass.
 * @param valueClass Class of the value associated with the inputFormatClass.
 * @param conf The Hadoop configuration.
 */
abstract class PreFilteredHadoopRDD[K, V](
    sc : SparkContext,
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    keyClass: Class[K],
    valueClass: Class[V],
    @transient conf: Configuration)
  extends RDD[(K, V)](sc, Nil)
  with SparkHadoopMapReduceUtil
  with Logging {

  // A Hadoop Configuration can be about 10 KB, which is pretty big, so broadcast it
  private val confBroadcast = sc.broadcast(new SerializableWritable(conf))
  // private val serializableConf = new SerializableWritable(conf)

  private val jobtrackerId: String = {
    val formatter = new SimpleDateFormat("yyyyMMddHHmm")
    formatter.format(new Date())
  }

  @transient private val jobId = new JobID(jobtrackerId, id)

  /**
   * returns true if specific partition has relevant keys
   */
  def includePartition(p: Partition): Boolean

  /**
   * returns true if the specific key in the partition passes the filter
   */
  def includeKey(key: K): Boolean

  override def getPartitions: Array[Partition] = {
    val inputFormat = inputFormatClass.newInstance
    if (inputFormat.isInstanceOf[Configurable]) {
      inputFormat.asInstanceOf[Configurable].setConf(conf)
    }
    val jobContext = newJobContext(conf, jobId)
    val rawSplits = inputFormat.getSplits(jobContext).toArray
    val result = new Array[Partition](rawSplits.size)
    for (i <- 0 until rawSplits.size) {
      result(i) = new NewHadoopPartition(id, i, rawSplits(i).asInstanceOf[InputSplit with Writable])
    }
    result.filter(includePartition)
  }

  override def compute(theSplit: Partition, context: TaskContext) = {
    val iter = new Iterator[(K, V)] {
      val split = theSplit.asInstanceOf[NewHadoopPartition]
      logInfo("Input split: " + split.serializableHadoopSplit)
      val conf = confBroadcast.value.value
      val attemptId = newTaskAttemptID(jobtrackerId, id, isMap = true, split.index, 0)
      val hadoopAttemptContext = newTaskAttemptContext(conf, attemptId)
      val format = inputFormatClass.newInstance
      if (format.isInstanceOf[Configurable]) {
        format.asInstanceOf[Configurable].setConf(conf)
      }
      val reader = format.createRecordReader(
        split.serializableHadoopSplit.value, hadoopAttemptContext)
      reader.initialize(split.serializableHadoopSplit.value, hadoopAttemptContext)

      // Register an on-task-completion callback to close the input stream.
      context.addOnCompleteCallback(() => close())
      var havePair = false
      var finished = false

      override def hasNext: Boolean = {
        //loop until a key passes our filter or we reach the end
        while (!finished && !havePair) {
          finished = !reader.nextKeyValue
          if (!finished)
            havePair = includeKey(reader.getCurrentKey)
        }
        !finished
      }

      override def next(): (K, V) = {
        if (!hasNext) {
          throw new java.util.NoSuchElementException("End of stream")
        }
        havePair = false
        (reader.getCurrentKey, reader.getCurrentValue)
      }

      private def close() {
        try {
          reader.close()
        } catch {
          case e: Exception => logWarning("Exception in RecordReader.close()", e)
        }
      }
    }
    new InterruptibleIterator(context, iter)
  }

  override def getPreferredLocations(split: Partition): Seq[String] = {
    val theSplit = split.asInstanceOf[NewHadoopPartition]
    theSplit.serializableHadoopSplit.value.getLocations.filter(_ != "localhost")
  }

  def getConf: Configuration = confBroadcast.value.value
}


//This is here because it is private[spark] in spark code base
import java.lang.{Integer => JInteger, Boolean => JBoolean}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.{TaskAttemptContext, TaskAttemptID, JobContext, JobID}

trait SparkHadoopMapReduceUtil {
  def newJobContext(conf: Configuration, jobId: JobID): JobContext = {
    val klass = firstAvailableClass(
      "org.apache.hadoop.mapreduce.task.JobContextImpl",  // hadoop2, hadoop2-yarn
      "org.apache.hadoop.mapreduce.JobContext")           // hadoop1
    val ctor = klass.getDeclaredConstructor(classOf[Configuration], classOf[JobID])
    ctor.newInstance(conf, jobId).asInstanceOf[JobContext]
  }

  def newTaskAttemptContext(conf: Configuration, attemptId: TaskAttemptID): TaskAttemptContext = {
    val klass = firstAvailableClass(
      "org.apache.hadoop.mapreduce.task.TaskAttemptContextImpl",  // hadoop2, hadoop2-yarn
      "org.apache.hadoop.mapreduce.TaskAttemptContext")           // hadoop1
    val ctor = klass.getDeclaredConstructor(classOf[Configuration], classOf[TaskAttemptID])
    ctor.newInstance(conf, attemptId).asInstanceOf[TaskAttemptContext]
  }

  def newTaskAttemptID(
    jtIdentifier: String,
    jobId: Int,
    isMap: Boolean,
    taskId: Int,
    attemptId: Int) =
  {
    val klass = Class.forName("org.apache.hadoop.mapreduce.TaskAttemptID")
    try {
      // First, attempt to use the old-style constructor that takes a boolean isMap
      // (not available in YARN)
      val ctor = klass.getDeclaredConstructor(classOf[String], classOf[Int], classOf[Boolean],
        classOf[Int], classOf[Int])
      ctor.newInstance(jtIdentifier, new JInteger(jobId), new JBoolean(isMap), new JInteger(taskId),
        new JInteger(attemptId)).asInstanceOf[TaskAttemptID]
    } catch {
      case exc: NoSuchMethodException => {
        // If that failed, look for the new constructor that takes a TaskType (not available in 1.x)
        val taskTypeClass = Class.forName("org.apache.hadoop.mapreduce.TaskType")
          .asInstanceOf[Class[Enum[_]]]
        val taskType = taskTypeClass.getMethod("valueOf", classOf[String]).invoke(
          taskTypeClass, if(isMap) "MAP" else "REDUCE")
        val ctor = klass.getDeclaredConstructor(classOf[String], classOf[Int], taskTypeClass,
          classOf[Int], classOf[Int])
        ctor.newInstance(jtIdentifier, new JInteger(jobId), taskType, new JInteger(taskId),
          new JInteger(attemptId)).asInstanceOf[TaskAttemptID]
      }
    }
  }

  private def firstAvailableClass(first: String, second: String): Class[_] = {
    try {
      Class.forName(first)
    } catch {
      case e: ClassNotFoundException =>
        Class.forName(second)
    }
  }
}
