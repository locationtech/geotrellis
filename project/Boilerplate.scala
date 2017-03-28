import sbt._

/**
 * Copied, with some modifications, from https://github.com/milessabin/shapeless/blob/master/project/Boilerplate.scala
 *
 * Generate a range of boilerplate classes, those offering alternatives with 0-22 params
 * and would be tedious to craft by hand
 */

object Boilerplate {
  import scala.StringContext._

  implicit final class BlockHelper(val sc: StringContext) extends AnyVal {
    def block(args: Any*): String = {
      val interpolated = sc.standardInterpolator(treatEscapes, args)
      val rawLines = interpolated split '\n'
      val trimmedLines = rawLines map { _ dropWhile (_.isWhitespace) }
      trimmedLines mkString "\n"
    }
  }

  val templatesMacro: Seq[Template] = Seq(
    GenIntTileCombinersFunctions,
    GenDoubleTileCombinersFunctions,
    GenMacroCombinableMultibandTile,
    GenMultibandTileMacros,
    GenMacroCombineFunctions)

  val templatesRaster: Seq[Template] = Seq(
    GenMacroMultibandCombiners,
    GenMacroSegmentCombiner,
    GenMacroGeotiffMultibandCombiners
  )

  val header = "// auto-generated boilerplate" // TODO: put something meaningful here?

  /** Returns a seq of the generated files.  As a side-effect, it actually generates them... */
  def gen(dir: File, templates: Seq[Template]) = for(t <- templates) yield {
    val tgtFile = t.filename(dir)
    IO.write(tgtFile, t.body)
    tgtFile
  }

  def genMacro(dir: File) = gen(dir, templatesMacro)
  def genRaster(dir: File) = gen(dir, templatesRaster)

  val maxArity = 10

  final class TemplateVals(val arity: Int) {
    def typedSeq(ts: String) = (0 until arity) map { i => s"b$i: $ts" } mkString ", "
    def typedVals(ts: String) = (0 until arity) map { i => s"val b$i: $ts" } mkString "; "
    def typedExprSeq(ts: String) = (0 until arity) map { i => s"b$i: c.Expr[$ts]" } mkString ", "
    def seq = (0 until arity) map { i => s"b$i" } mkString ", "
    def namedSeq(n: String) = (0 until arity) map { i => s"$n" } mkString ", "
  }

  trait Template {
    def filename(root: File): File
    def content(tv: TemplateVals): String
    def range = 1 to maxArity
    def body: String = {
      val headerLines = header split '\n'
      val rawContents = range map { n => content(new TemplateVals(n)) split '\n' filterNot (_.isEmpty) }
      val preBody = rawContents.head takeWhile (_ startsWith "|") map (_.tail)
      val instances = rawContents flatMap {_ filter (_ startsWith "-") map (_.tail) }
      val postBody = rawContents.head dropWhile (_ startsWith "|") dropWhile (_ startsWith "-") map (_.tail)
      (headerLines ++ preBody ++ instances ++ postBody) mkString "\n"
    }
  }

  /*
    Blocks in the templates below use a custom interpolator, combined with post-processing to produce the body

      - The contents of the `header` val is output first

      - Then the first block of lines beginning with '|'

      - Then the block of lines beginning with '-' is replicated once for each arity,
        with the `templateVals` already pre-populated with relevant relevant vals for that arity

      - Then the last block of lines prefixed with '|'

    The block otherwise behaves as a standard interpolated string with regards to variable substitution.
  */

  trait TileCombinerFunctions extends Template {
    val typeString: String
    def filename(root: File) = root / "geotrellis" / "macros" / s"${typeString}TileCombiners.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val args = typedSeq(typeString)
      val vals = typedVals("Int")

      block"""
         |package geotrellis.macros
        -  trait ${typeString}TileCombiner${arity} {
        -  ${vals}
        -  def apply(${args}): ${typeString}
        -  }
      """
    }
  }

  object GenIntTileCombinersFunctions extends TileCombinerFunctions {
    val typeString = "Int"
  }

  object GenDoubleTileCombinersFunctions extends TileCombinerFunctions {
    val typeString = "Double"
  }

  object GenMacroCombinableMultibandTile extends Template {
    def filename(root: File) = root / "geotrellis" / "macros" / "MacroCombinableMultibandTile.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      block"""
         |package geotrellis.macros
         |trait MacroCombinableMultibandTile[T] {
        -  def combineIntTileCombiner(combiner: IntTileCombiner${arity}): T
        -  def combineDoubleTileCombiner(combiner: DoubleTileCombiner${arity}): T
         |}
      """
    }
  }

  object GenMultibandTileMacros extends Template {
    def filename(root: File) = root / "geotrellis" / "macros" / "MultibandTileMacros.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val argsInt    = typedSeq("Int")
      val valsInt    = typedVals("Int")
      val exprSeqInt = typedExprSeq("Int")
      val seqInt     = namedSeq("Int")
      val argsDouble = typedSeq("Double")
      val valsDouble = typedVals("Double")
      val seqDouble  = namedSeq("Double")

      def exprFunc(ts: String) = s"c.Expr[(${namedSeq(ts)}) => $ts]"
      val exprFArgsInt         = exprFunc("Int")
      val exprFArgsDouble      = exprFunc("Double")

      val quoted = (0 until arity) map { i => s"val b$i = $$b$i" } mkString "; "

      block"""
         |package geotrellis.macros
         |import spire.macros.InlineUtil
         |import scala.reflect.macros.whitebox.Context
         |import scala.language.experimental.macros
         |object MultibandTileMacros {
        -  def intCombine${arity}_impl[T, MBT <: MacroCombinableMultibandTile[T]](c: Context)(${exprSeqInt})(f: ${exprFArgsInt}): c.Expr[T] = {
        -    import c.universe._
        -    val self = c.Expr[MacroCombinableMultibandTile[T]](c.prefix.tree)
        -    val tree =
        -    q\"\"\"$$self.combineIntTileCombiner(new geotrellis.macros.IntTileCombiner${arity} {
        -       ${quoted}
        -       def apply(${argsInt}): Int = $$f(${seq})
        -    })\"\"\"
        -    new InlineUtil[c.type](c).inlineAndReset[T](tree)
        -  }
        -  def doubleCombine${arity}_impl[T, MBT <: MacroCombinableMultibandTile[T]](c: Context)(${exprSeqInt})(f: ${exprFArgsDouble}): c.Expr[T] = {
        -    import c.universe._
        -    val self = c.Expr[MacroCombinableMultibandTile[T]](c.prefix.tree)
        -    val tree =
        -    q\"\"\"$$self.combineDoubleTileCombiner(new geotrellis.macros.DoubleTileCombiner${arity} {
        -       ${quoted}
        -       def apply(${argsDouble}): Double = $$f(${seq})
        -    })\"\"\"
        -    new InlineUtil[c.type](c).inlineAndReset[T](tree)
        -  }
         |}
      """
    }
  }

  object GenMacroCombineFunctions extends Template {
    def filename(root: File) = root / "geotrellis" / "macros" / "MacroCombineFunctions.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val argsInt    = typedSeq("Int")
      val argsDouble = typedSeq("Double")

      def seqFunc(ts: String) = s"(${namedSeq(ts)}) => $ts"
      def seqFInt             = seqFunc("Int")
      def seqFDouble          = seqFunc("Double")

      block"""
         |package geotrellis.macros
         |import scala.language.experimental.macros
         |trait MacroCombineFunctions[T, MBT <: MacroCombinableMultibandTile[T]] {
        -  def combine(${argsInt})(f: ${seqFInt}): T =
        -    macro MultibandTileMacros.intCombine${arity}_impl[T, MBT]
        -  def combineDouble(${argsInt})(f: ${seqFDouble}): T =
        -    macro MultibandTileMacros.doubleCombine${arity}_impl[T, MBT]
         |}
      """
    }
  }

  object GenMacroMultibandCombiners extends Template {
    def filename(root: File) = root / "geotrellis" / "raster" / "MacroMultibandCombiners.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val bandVals = (0 until arity) map { i => s"val band$i = band(combiner.b$i)" } mkString "; "
      val bandIntArgs = (0 until arity) map { i => s"band$i.get(col, row)" } mkString ", "
      val bandDoubleArgs = (0 until arity) map { i => s"band$i.getDouble(col, row)" } mkString ", "

      block"""
         |package geotrellis.raster
         |import geotrellis.macros._
         |import spire.syntax.cfor._
         |trait MacroMultibandCombiners { self: MultibandTile =>
        -  def combineIntTileCombiner(combiner: IntTileCombiner${arity}): Tile = {
        -    ${bandVals}
        -    val result = ArrayTile.empty(targetCellType, cols, rows)
        -    val arr = Array.ofDim[Int](bandCount)
        -    cfor(0)(_ < rows, _ + 1) { row =>
        -      cfor(0)(_ < cols, _ + 1) { col =>
        -        result.set(col, row, combiner(${bandIntArgs}))
        -      }
        -    }
        -    result
        -  }
        -  def combineDoubleTileCombiner(combiner: DoubleTileCombiner${arity}): Tile = {
        -    ${bandVals}
        -    val result = ArrayTile.empty(targetCellType, cols, rows)
        -    val arr = Array.ofDim[Int](bandCount)
        -    cfor(0)(_ < rows, _ + 1) { row =>
        -      cfor(0)(_ < cols, _ + 1) { col =>
        -        result.setDouble(col, row, combiner(${bandDoubleArgs}))
        -      }
        -    }
        -    result
        -  }
         |}
      """
    }
  }

object GenMacroSegmentCombiner extends Template {
    def filename(root: File) = root / "geotrellis" / "raster" / "SegmentCombiner.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val sArgs = (1 to arity) map { i => s"s$i: GeoTiffSegment, i$i: Int" } mkString ", "
      val zsVals = (1 to arity) map { i => s"val z$i = s$i.getInt(i$i)" } mkString "; "
      val zArgs = (1 to arity) map { i => s"z$i" } mkString ", "

      block"""
         |package geotrellis.raster
         |import geotrellis.macros._
         |import geotrellis.raster.io.geotiff._
         |import geotrellis.raster.io.geotiff.compression._
         |import spire.syntax.cfor._
         | /** This trait is how subclasses define the necessary pieces that allow
         | * us to abstract over each of the combine functions */
         | abstract class SegmentCombiner(bandCount: Int) {
         |   private var valueHolder: Array[Int] = null
         |   private var valueHolderDouble: Array[Double] = null
         |   def initValueHolder(): Unit = { valueHolder = Array.ofDim[Int](bandCount) }
         |   def initValueHolderDouble(): Unit = { valueHolderDouble = Array.ofDim[Double](bandCount) }
         |   def set(targetIndex: Int, v: Int): Unit
         |   def setDouble(targetIndex: Int, v: Double): Unit
         |   def set(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int)
         |   (f: (Int, Int) => Int): Unit = {
         |     val z1 = s1.getInt(i1)
         |     val z2 = s2.getInt(i2)
         |     set(targetIndex, f(z1, z2))
         |   }
         |   def setDouble(targetIndex: Int, s1: GeoTiffSegment, i1: Int, s2: GeoTiffSegment, i2: Int)
         |   (f: (Double, Double) => Double): Unit = {
         |     val z1 = s1.getDouble(i1)
         |     val z2 = s2.getDouble(i2)
         |     setDouble(targetIndex, f(z1, z2))
         |   }
         |   // Used for combining all bands.
         |   def placeValue(segment: GeoTiffSegment, i: Int, bandIndex: Int): Unit = {
         |     valueHolder(bandIndex) = segment.getInt(i)
         |   }
         |   def setFromValues(targetIndex: Int, f: Array[Int] => Int): Unit = {
         |     set(targetIndex, f(valueHolder))
         |   }
         |   def placeValueDouble(segment: GeoTiffSegment, i: Int, bandIndex: Int): Unit = {
         |     valueHolderDouble(bandIndex) = segment.getDouble(i)
         |   }
         |   def setFromValuesDouble(targetIndex: Int, f: Array[Double] => Double): Unit = {
         |     setDouble(targetIndex, f(valueHolderDouble))
         |   }
         |   def getBytes(): Array[Byte]
         -   def set(targetIndex: Int, ${sArgs})(combiner: IntTileCombiner${arity}): Unit = {
         -     ${zsVals}
         -     set(targetIndex, combiner(${zArgs}))
         -   }
         -   def setDouble(targetIndex: Int, ${sArgs})(combiner: DoubleTileCombiner${arity}): Unit = {
         -     ${zsVals}
         -     setDouble(targetIndex, combiner(${zArgs}))
         -   }
         | }
      """
    }
  }

  object GenMacroGeotiffMultibandCombiners extends Template {
    def filename(root: File) = root / "geotrellis" / "raster" / "MacroGeotiffMultibandCombiners.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val argsInt = typedSeq("Int")
      val tupArgs = (0 until arity) map { i => "GeoTiffSegment, Int" } mkString ", "
      val asserts = (0 until arity) map { i =>
        s"""assert(b$i < bandCount, s"Illegal band index: $$b$i is out of range ($$bandCount bands)")"""
      } mkString "; "

      val diffs        = (1 until arity) map { i => s"val diff$i = b$i - b0 "} mkString "; "
      val diffsArgs    = ((((1 until arity) map { i => s"i + diff$i, segment" } mkString ", ") split ", ") init) mkString ", "
      val startVals    = (0 until arity) map { i => s"val start$i = bandSegmentCount * b$i" } mkString "; "
      val getSegmentsZip = (0 until arity) map { i =>
        if(i == 0) s"getSegments(start$i until start$i + bandSegmentCount)" else s"zip(getSegments(start$i until start$i + bandSegmentCount))"
      } mkString "."
      val bracesForZipArgs = (0 until arity - 1) map { _ => "(" } mkString ""
      val segmentsZipArgs = bracesForZipArgs concat ((0 until arity) map { i => if(i == 0) s"(segmentIndex$i, segment$i)" else s"(_, segment$i))" } mkString ", ")
      val segmentArgs  = (0 until arity) map { i => s"i, segment$i" } mkString ", "
      val combinerArgs = (0 until arity) map { i => s"combiner.b$i" } mkString ", "
      val siArgs       = (1 to arity) map { i => s"s$i: GeoTiffSegment, i$i: Int" } mkString ", "
      val siTypedArgs  = (1 to arity) map { i => s"s$i, i$i" } mkString ", "
      val zsVals       = (1 to arity) map { i => s"val z$i = s$i.getInt(i$i)" } mkString "; "

      block"""
         |package geotrellis.raster
         |import geotrellis.macros._
         |import geotrellis.raster.io.geotiff._
         |import geotrellis.raster.io.geotiff.compression._
         |import spire.syntax.cfor._
         |trait MacroGeotiffMultibandCombiners {
         |  def cellType: CellType
         |  def getSegment(i: Int): GeoTiffSegment
         |  def getSegments(ids: Traversable[Int]): Iterator[(Int, GeoTiffSegment)]
         |
         |  val segmentLayout: GeoTiffSegmentLayout
         |  val segmentCount: Int
         |  val bandCount: Int
         |  val compression: Compression
         |
         | /** Creates a segment combiner, which is an abstraction that allows us to generalize
         | * the combine algorithms over BandType. */
         | protected def createSegmentCombiner(targetSize: Int): SegmentCombiner
         |
        -  def combineDoubleTileCombiner(combiner: DoubleTileCombiner${arity}): Tile =
        -   _combine(${combinerArgs}) { segmentCombiner =>
        -     { (targetIndex: Int, ${siArgs}) => segmentCombiner.setDouble(targetIndex, ${siTypedArgs})(combiner) } }
        -  def combineIntTileCombiner(combiner: IntTileCombiner${arity}): Tile =
        -   _combine(${combinerArgs}) { segmentCombiner =>
        -     { (targetIndex: Int, ${siArgs}) => segmentCombiner.set(targetIndex, ${siTypedArgs})(combiner) } }
        -
        -  protected def _combine(${argsInt})(set: SegmentCombiner => (Int, ${tupArgs}) => Unit): Tile = {
        -    ${asserts}
        -    val (arr, compressor) =
        -      if(segmentLayout.hasPixelInterleave) {
        -        ${diffs}
        -        val compressor = compression.createCompressor(segmentCount)
        -        val arr = Array.ofDim[Array[Byte]](segmentCount)
        -        getSegments(0 until segmentCount).foreach { case (segmentIndex, segment) =>
        -          val segmentSize = segment.size
        -          val segmentCombiner = createSegmentCombiner(segmentSize / bandCount)
        -          var j = 0
        -          cfor(b0)(_ < segmentSize, _ + bandCount) { i =>
        -            set(segmentCombiner)(j, segment, i, segment, ${diffsArgs})
        -            j += 1
        -          }
        -          arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        -        }
        -        (arr, compressor)
        -    } else {
        -      val bandSegmentCount = segmentCount / bandCount
        -      val compressor = compression.createCompressor(bandSegmentCount)
        -      val arr = Array.ofDim[Array[Byte]](bandSegmentCount)
        -      ${startVals}
        -      ${getSegmentsZip}.foreach { case (${segmentsZipArgs}) =>
        -        val segmentIndex = segmentIndex0 - start0
        -        val segmentSize = segment0.size
        -        val segmentCombiner = createSegmentCombiner(segmentSize)
        -        cfor(0)(_ < segmentSize, _ + 1) { i =>
        -          set(segmentCombiner)(${segmentArgs}, i)
        -        }
        -        arr(segmentIndex) = compressor.compress(segmentCombiner.getBytes, segmentIndex)
        -      }
        -      (arr, compressor)
        -    }
        -    GeoTiffTile(
        -      new ArraySegmentBytes(arr),
        -      compressor.createDecompressor(),
        -      segmentLayout,
        -      compression,
        -      cellType
        -    )
        -  }
         |}
      """
    }
  }
}
