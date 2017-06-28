0004 - Spark Streaming
----------------------

Context
^^^^^^^

During the current ``GeoTrellis`` Ingest process, data is ingested as a huge chunk of data. During Ingest the tiling shuffle
breaks the memory allocation the process is very likely to crash. One of reasons to investigate Streaming to ingest data incrementally,
definitely that would be slower, as it requires layer *update*. Another point that Spark Streaming supports checkpoiniting:
potentially we can persist application context and in case of failure to recover context.


Decision
^^^^^^^^

Was implemented a beta API and we tried to use ``Spark Streaming`` in `Landsat EMR Demo project <https://github.com/pomadchin/geotrellis-landsat-emr-demo/tree/feature/streaming>`_.

API
^^^

``Spark Streaming`` API provides easy wrapping / reusing of GeoTrellis RDD functions, as ``DStream`` implements:

.. code:: scala

  def foreachRDD(foreachFunc: (RDD[T]) ⇒ Unit): Unit
  def transform[U](transformFunc: (RDD[T]) ⇒ RDD[U])(implicit arg0: ClassTag[U]): DStream[U]
  def transformWith[U, V](other: DStream[U], transformFunc: (RDD[T], RDD[U]) ⇒ RDD[V])(implicit arg0: ClassTag[U], arg1: ClassTag[V]): DStream[V]


And other higher ordered functions, which makes possible GeoTrellis ``RDD`` functions usage without reimplementing,
but by just with wrapping our core api.

Basic terms and important `Spark Streaming <http://spark.apache.org/docs/latest/configuration.html>`_ setting to control performance:
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* ``Batch interval`` - the interval at which the streaming API will update (socket / folder / receiver period lookup) data
* ``Window`` - data between times
* ``receiver.maxRate`` - maximum rate (number of records per second) at which each receiver will receive data
* ``backpressure.enabled`` - in fact just dynamically sets ``receiver.maxRate``, upper bounded ``receiver.maxRate`` that can be processed by cluster
* ``Streaming Events`` - some events that can be set (exmpl. on microbatch complete), description would be provided below

At first the idea was to ingest tiles as chunked batches and to process these batches sequentially. Instead of a common writer was used ``WriterOrUpdater``.
The problem was to control somehow the stream, and to have only one "batch" processed. But it turned out that it is not
possible via standard Spark Streaming API. The problem is that input batch immediately splited into microbatches and into lots of jobs,
and it is not possible to understand the real state of the current ingest process.

The consequence of ``Spark Streaming`` usage was just slowing down the ingest process (as input was parallelized, and instead of just
one write, in a common ingest process, ``WriteOrUpdate`` was called every time, and mostly ``Update`` was used).

As a workaround was tried to setup listeners on a ``DStream``:

.. code:: scala

  trait StreamingListener {
    /** Called when a receiver has been started */
    def onReceiverStarted(receiverStarted: StreamingListenerReceiverStarted): Unit

    /** Called when a receiver has reported an error */
    def onReceiverError(receiverError: StreamingListenerReceiverError): Unit

    /** Called when a receiver has been stopped */
    def onReceiverStopped(receiverStopped: StreamingListenerReceiverStopped): Unit

    /** Called when a batch of jobs has been submitted for processing. */
    def onBatchSubmitted(batchSubmitted: StreamingListenerBatchSubmitted): Unit

    /** Called when processing of a batch of jobs has started.  */
    def onBatchStarted(batchStarted: StreamingListenerBatchStarted): Unit

    /** Called when processing of a batch of jobs has completed. */
    def onBatchCompleted(batchCompleted: StreamingListenerBatchCompleted): Unit

    /** Called when processing of a job of a batch has started. */
    def onOutputOperationStarted(
      outputOperationStarted: StreamingListenerOutputOperationStarted): Unit

    /** Called when processing of a job of a batch has completed. */
    def onOutputOperationCompleted(
      outputOperationCompleted: StreamingListenerOutputOperationCompleted): Unit
  }

The most interesting events are ``onBatchCompleted`` an ``onOutputOperationCompleted``, but they called not on the *input* batch, but on microbatches (empirically proofed information).


Conclusion
^^^^^^^^^^

The use case of controlling memory and the Ingest process is possible, but not using ``Spark Streaming`` only.
The logic of holding the input batch should be handled by some additional service, and in fact that means that we can't control
directly memory usage using ``Spark Streaming``. However it is still an interesting tool and we have a use case of
monitoring some folder / socket / other source and receiving tiles in some batches periodically and potentially,
an interesting ``Feature`` Ingest API can be implemented.

As a solution for `Landsat EMR Demo project <https://github.com/pomadchin/geotrellis-landsat-emr-demo/tree/feature/streaming>`_
it is possible to set up certain batches (example: by 5 tiles), and certain batch interval (example: each 10 minutes),
but that solution not prevents application from increased memory usage.
