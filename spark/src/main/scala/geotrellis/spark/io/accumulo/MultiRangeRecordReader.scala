package geotrellis.spark.io.accumulo

import org.apache.accumulo.core.client._
import org.apache.accumulo.core.data._
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.mapreduce.{RecordReader, TaskAttemptContext, InputSplit}
import scala.collection.JavaConverters._

/**
 * It is not clear what would be better, a series of scanners or a BatchScanner.
 * BatchScanner is way easier to code for this, so that's what's happening.
 */
class MultiRangeRecordReader extends RecordReader[Key, Value] {
  var scanner: BatchScanner = null
  var iterator: Iterator[java.util.Map.Entry[Key, Value]] = null
  var key: Key = null
  var value: Value = null

  def initialize(inputSplit: InputSplit, context: TaskAttemptContext): Unit = {    
    val split = inputSplit.asInstanceOf[MultiRangeInputSplit]
    
    val queryThreads = 1
    val connector = split.connector
    scanner = connector.createBatchScanner(split.table, new Authorizations(), queryThreads);
    scanner.setRanges(split.ranges.asJava)        
    split.iterators foreach { scanner.addScanIterator }
    split.fetchedColumns foreach { pair =>  
      if (pair.getSecond != null)
        scanner.fetchColumn(pair.getFirst, pair.getSecond) 
      else 
        scanner.fetchColumnFamily(pair.getFirst)
    }
    iterator = scanner.iterator().asScala
  }

  def getProgress: Float = ???

  def nextKeyValue(): Boolean = {
    val hasNext = iterator.hasNext
    if (hasNext) {
      val entry = iterator.next()
      key = entry.getKey
      value = entry.getValue
    }
    hasNext
  }

  def getCurrentValue: Value = value

  def getCurrentKey: Key = key

  def close(): Unit = {
    scanner.close
  }
}