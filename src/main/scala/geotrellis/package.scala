import geotrellis.source.CanBuildSourceFrom
import geotrellis.statistics.Histogram

package object geotrellis {
  final val NODATA = Int.MinValue

  type Op[+A] = Operation[A]
  type DI = DummyImplicit

  type RasterData = geotrellis.raster.RasterData

  type RasterDS = geotrellis.source.RasterDataSource
  type SeqDS[E] = geotrellis.source.DataSource[E,Seq[E]]
  type HistogramDS = geotrellis.source.DataSource[Histogram,Histogram]

  @inline def isNaN(d:Double) = java.lang.Double.isNaN(d)

  /**
   * Add simple syntax for creating an operation.
   *
   * Define a function after op function that returns:
   *
   * 1) A literal value, e.g.
   *   val plusOne = op { (i:Int) => i + 1 }
   *
   * 2) An operation to be executed:
   *   val localPlusOne = ( (r:Raster, i:Int) => local.Add(r,i + 1) )
   *
   * 3) Or a StepResult (which indicates success or failure)
   *   val plusOne = op { (i:Int) => Result(i + 1) }
   *
   */

  // Op1 methods for op //

  /**
   * Create an operation from a 1-arg function that returns StepOutput.
   *
   * For example:
   *
   * val plusOne = op { (i:Int) => Result(i + 1) }
   */
  def op[A, T](f: (A) => StepOutput[T]): (Op[A]) => Op1[A, T] =
    (a: Op[A]) => new Op1(a)((a) => f(a))

  /**
   * Create an operation from a 1-arg function that returns an operation to be executed.
   *
   * For example:
   *
   * val localPlusOne = ( (r:Raster, i:Int) => local.Add(r,i + 1) )
   *
   */
  def op[A, T](f: (A) => Op[T])(implicit n: DI): (Op[A]) => Op1[A, T] =
    (a: Op[A]) => new Op1(a)((a) => StepRequiresAsync(List(f(a)), (l) => Result(l.head.asInstanceOf[T])))

  /**
   * Create an operation from a 1-arg function that returns a literal value.
   *
   * For example:
   *
   * val plusOne = op { (i:Int) => i + 1 }
   */
  def op[A, T](f: (A) => T)(implicit n: DI, o: DI): (Op[A]) => Op1[A, T] =
    (a: Op[A]) => new Op1(a)((a) => Result(f(a)))

  // Op2 methods for op() //

  /**
   * Create an operation from a 2-arg function that returns StepOutput.
   */
  def op[A, B, T](f: (A, B) => StepOutput[T]): (Op[A], Op[B]) => Op2[A, B, T] =
    (a: Op[A], b: Op[B]) => new Op2(a, b)((a, b) => f(a, b))

  /**
   * Create an operation from a 2-arg function that returns an operation.
   */
  def op[A, B, T](f: (A, B) => Op[T])(implicit n: DI): (Op[A], Op[B]) => Op2[A, B, T] =
    (a: Op[A], b: Op[B]) => new Op2(a, b)((a, b) => StepRequiresAsync(List(f(a, b)), (l) =>
      Result(l.head.asInstanceOf[T])))

  /**
   * Create an operation from a 2-arg function that returns a literal value.
   */
  def op[A, B, T](f: (A, B) => T)(implicit n: DI, o: DI): (Op[A], Op[B]) => Op2[A, B, T] =
    (a: Op[A], b: Op[B]) => new Op2(a, b)((a, b) => Result(f(a, b)))

  // Op3 methods for op() //

  /**
   * Create an operation from a 3-arg function that returns StepOutput.
   */
  def op[A, B, C, T](f: (A, B, C) => StepOutput[T]): (Op[A], Op[B], Op[C]) => Op3[A, B, C, T] =
    (a: Op[A], b: Op[B], c: Op[C]) => new Op3(a, b, c)((a, b, c) => f(a, b, c))

  /**
   * Create an operation from a 3-arg function that returns an operation.
   */
  def op[A, B, C, T](f: (A, B, C) => Op[T])(implicit  n: DI): (Op[A], Op[B], Op[C]) => Op3[A, B, C, T] =
    (a: Op[A], b: Op[B], c: Op[C]) =>
      new Op3(a, b, c)((a, b, c) => StepRequiresAsync(List(f(a, b, c)), (l) => Result(l.head.asInstanceOf[T])))

  /**
   * Create an operation from a 3-arg function that returns a literal value.
   */
  def op[A, B, C, T](f: (A, B, C) => T)(implicit n: DI, o: DI): (Op[A], Op[B], Op[C]) => Op3[A, B, C, T] =
    (a: Op[A], b: Op[B], c: Op[C]) => new Op3(a, b, c)((a, b, c) => Result(f(a, b, c)))

  // Op4 methods for op() //

  /**
   * Create an operation from a 4-arg function that returns StepOutput.
   */
  def op[A, B, C, D, T](f: (A, B, C, D) => StepOutput[T]): 
	  (Op[A], Op[B], Op[C], Op[D]) => Op4[A, B, C, D, T] =
    (a: Op[A], b: Op[B], c: Op[C], d: Op[D]) => new Op4(a, b, c, d)((a, b, c, d) => f(a, b, c, d))

  /**
   * Create an operation from a 4-arg function that returns an operation.
   */
  def op[A, B, C, D, T](f: (A, B, C, D) => Op[T])(implicit n: DI): 
	  (Op[A], Op[B], Op[C], Op[D]) => Op4[A, B, C, D, T] =
    (a: Op[A], b: Op[B], c: Op[C], d: Op[D]) => new Op4(a, b, c, d)((a, b, c, d) => 
      StepRequiresAsync(List(f(a, b, c, d)), (l) => Result(l.head.asInstanceOf[T])))
  
  /**
   * Create an operation from a 4-arg function that returns a literal value.
   */
  def op[A, B, C, D, T](f: (A, B, C, D) => T)(implicit n: DI, o: DI): 
	  (Op[A], Op[B], Op[C], Op[D]) => Op4[A, B, C, D, T] =
    (a: Op[A], b: Op[B], c: Op[C], d: Op[D]) => new Op4(a, b, c, d)((a, b, c, d) => 
      Result(f(a, b, c, d)))

  /**
   * Execute operation with temporary server instance.
   */
  def execute[T:Manifest](op:Operation[T]) = {
    val s = process.Server.empty("execute")
    val result = s.run(op)
    s.shutdown()
    result 
  }

  /**
   * Syntax for converting tuples of operations
   * into objects that you can call map and flatMap on.
   * this is similiar to a for comprehension, but the
   * operations will be executed in parallel.
   */
  implicit class OpMap2[A,B](t:(Op[A],Op[B])) {
    def map[T](f:(A,B)=>T) = op(f).apply(t._1,t._2)
    def flatMap[T](f:(A,B)=>Op[T]) = op(f).apply(t._1,t._2)
  }

  implicit class OpMap3[A,B,C](t:(Op[A],Op[B],Op[C])) {
    def map[T](f:(A,B,C)=>T) = op(f).apply(t._1,t._2,t._3)
    def flatMap[T](f:(A,B,C)=>Op[T]) = op(f).apply(t._1,t._2,t._3)
  }

  implicit class OpMap4[A,B,C,D](t:(Op[A],Op[B],Op[C],Op[D])) {
    def map[T](f:(A,B,C,D)=>T) = op(f).apply(t._1,t._2,t._3,t._4)
    def flatMap[T](f:(A,B,C,D)=>Op[T]) = op(f).apply(t._1,t._2,t._3,t._4)
  }

  /** 
   * Syntax for converting an iterable collection to 
   * have methods to work with the results of those 
   * operations executed in parallel
   */
  implicit class OpMapSeq[A](seq:Seq[Op[A]]) {
    def mapOps[T](f:(Seq[A]=>T)) = logic.Collect(Literal(seq)).map(f)
    def flaMapOps[T](f:(Seq[A]=>Op[T])) = logic.Collect(Literal(seq)).flatMap(f)
  }

  implicit class OpMapArray[A](seq:Array[Op[A]]) {
    def mapOps[T](f:(Seq[A]=>T)) = logic.Collect(Literal(seq.toSeq)).map(f)
    def flaMapOps[T](f:(Seq[A]=>Op[T])) = logic.Collect(Literal(seq.toSeq)).flatMap(f)
  }

  implicit class OpSeqToCollect[T](seq:Op[Seq[Op[T]]]) {
    def collect() = logic.Collect(seq)
  }
}
