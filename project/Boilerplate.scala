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
    GenMacroCombinableMultiBandTile,
    GenMultiBandTileMacros,
    GenMacroCombineFunctions
  )

  val templatesRaster: Seq[Template] = Seq(GenMacroMultibandCombiners)

  val header = "// auto-generated boilerplate" // TODO: put something meaningful here?

  /** Returns a seq of the generated files.  As a side-effect, it actually generates them... */
  def gen(dir: File, templates: Seq[Template]) = for(t <- templates) yield {
    val tgtFile = t.filename(dir)
    IO.write(tgtFile, t.body)
    tgtFile
  }

  def genMacro(dir: File) = gen(dir, templatesMacro)
  def genRaster(dir: File) = gen(dir, templatesRaster)

  val maxArity = 22

  final class TemplateVals(val arity: Int) {
    def synArgs(ts: String) = (0 until arity) map { i => s"b$i: $ts" } mkString ", "
    def synVals(ts: String) = (0 until arity) map { i => s"val b$i: $ts"} mkString "; "
    def synExprArgs(ts: String) = (0 until arity) map { i => s"b$i: c.Expr[$ts]"} mkString ", "
    def tupleSyn = (0 until arity) map { i => s"b$i"} mkString ", "
    def tuple(ts: String) = (0 until arity) map { i => s"$ts"} mkString ", "
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
      
      val args = synArgs(typeString)
      val vals = synVals("Int")
      
      block"""
         |package geotrellis.macros
        -  trait ${typeString}TileCombiner${arity} {
        -  $vals
        -  def apply($args): ${typeString}
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

  object GenMacroCombinableMultiBandTile extends Template {    
    def filename(root: File) = root / "geotrellis" / "macros" / "MacroCombinableMultiBandTile.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._
      
      block"""
         |package geotrellis.macros
         |trait MacroCombinableMultiBandTile[T] {
        -  def combineIntTileCombiner(combiner: IntTileCombiner$arity): T
        -  def combineDoubleTileCombiner(combiner: DoubleTileCombiner$arity): T        
         |}        
      """
    }
  }

  object GenMultiBandTileMacros extends Template {    
    def filename(root: File) = root / "geotrellis" / "macros" / "MultiBandTileMacros.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val argsInt     = synArgs("Int")
      val valsInt     = synVals("Int")
      val exprInt     = synExprArgs("Int")
      val tupleInt    = tuple("Int")
      val argsDouble  = synArgs("Double")
      val valsDouble  = synVals("Double")
      val tupleDouble = tuple("Double")

      val quoted = (0 until arity) map { i => s"val b$i = $$b$i" } mkString "; "

      block"""
         |package geotrellis.macros
         |import spire.macros.InlineUtil
         |import scala.reflect.macros.Context
         |import scala.language.experimental.macros
         |object MultiBandTileMacros {
        -  def intCombine${arity}_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)($exprInt)(f: c.Expr[($tupleInt) => Int]): c.Expr[T] = {
        -    import c.universe._
        -    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
        -    val tree = 
        -    q\"\"\"$$self.combineIntTileCombiner(new geotrellis.macros.IntTileCombiner$arity {
        -       $quoted
        -       def apply($argsInt): Int = $$f(${tupleSyn})
        -    })\"\"\"
        -    new InlineUtil[c.type](c).inlineAndReset[T](tree)
        -  }
        -  def doubleCombine${arity}_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)($exprInt)(f: c.Expr[($tupleDouble) => Double]): c.Expr[T] = {
        -    import c.universe._
        -    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
        -    val tree = 
        -    q\"\"\"$$self.combineDoubleTileCombiner(new geotrellis.macros.DoubleTileCombiner$arity {
        -       $quoted
        -       def apply($argsDouble): Double = $$f(${tv.tupleSyn})
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

      val argsInt     = synArgs("Int")            
      val tupleInt    = tuple("Int")
      val argsDouble  = synArgs("Double")      
      val tupleDouble = tuple("Double")

      block"""
         |package geotrellis.macros
         |import scala.language.experimental.macros
         |trait MacroCombineFunctions[T, MBT <: MacroCombinableMultiBandTile[T]] {
        -  def combine($argsInt)(f: ($tupleInt) => Int): T =
        -    macro MultiBandTileMacros.intCombine${arity}_impl[T, MBT]
        -  def combineDouble($argsInt)(f: ($tupleDouble) => Double): T =
        -    macro MultiBandTileMacros.doubleCombine${arity}_impl[T, MBT]
         |}        
      """
    }
  }

  object GenMacroMultibandCombiners extends Template {
    def filename(root: File) = root / "geotrellis" / "macros" / "MacroMultibandCombiners.scala"
    override def range = 3 to maxArity
    def content(tv: TemplateVals) = {
      import tv._

      val argsInt     = synArgs("Int")
      val tupleInt    = tuple("Int")
      val argsDouble  = synArgs("Double")
      val tupleDouble = tuple("Double")

      val bandVals = (0 until arity) map { i => s"val band$i = band(combiner.b$i)" } mkString "; "
      val bandArgs = (0 until arity) map { i => s"band$i.get(col, row)" } mkString ", "
      val bandDoubleArgs = (0 until arity) map { i => s"band$i.getDouble(col, row)" } mkString ", "

      block"""
         |package geotrellis.raster    
         |import spire.syntax.cfor._              
         |trait MacroMultibandCombiners { self: MultiBandTile =>      
        -  def combineIntTileCombiner(combiner: macros.IntTileCombiner$arity): Tile = {
        -  $bandVals
        -  val result = ArrayTile.empty(cellType, cols, rows)
        -  val arr = Array.ofDim[Int](bandCount)
        -  cfor(0)(_ < rows, _ + 1) { row =>
        -    cfor(0)(_ < cols, _ + 1) { col =>
        -      result.set(col, row, combiner($bandArgs))
        -    }
        -  }
        -  result
        -  }
        -  def combineDoubleTileCombiner(combiner: macros.DoubleTileCombiner$arity): Tile = {
        -  $bandVals
        -  val result = ArrayTile.empty(cellType, cols, rows)
        -  val arr = Array.ofDim[Int](bandCount)
        -  cfor(0)(_ < rows, _ + 1) { row =>
        -    cfor(0)(_ < cols, _ + 1) { col =>
        -      result.setDouble(col, row, combiner($bandDoubleArgs))
        -    }
        -  }
        -  result
        -  }
         |}
      """
    }
  }
}