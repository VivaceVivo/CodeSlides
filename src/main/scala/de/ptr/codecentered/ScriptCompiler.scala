package de.ptr.codecentered

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URI
import java.net.URLClassLoader
import java.util.Arrays
import scala.tools.nsc._
import scala.tools.nsc.interpreter.IMain
import javax.tools.JavaCompiler.CompilationTask
import javax.tools.JavaFileObject
import javax.tools.JavaFileObject.Kind
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import java.io.File
import java.net.URL
import java.io.PrintWriter
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import sun.misc.IOUtils
import java.net.ServerSocket
import scala.io.Source

// http://stackoverflow.com/questions/27470669/scala-reflect-internal-fatalerror-package-scala-does-not-have-a-member-int/29582915#29582915
case class CompileResult(console: String, result: String)

trait Probe

class ScriptCompiler {
  // the output lines starting with these prefixes should not be shown:
  val filterOut = List("import ", "out: ", "synchronousExecutionContext: ")

  // optionally set different Output Style (more verbose)
  //  settings processArgumentString "-Xprint:typer"

  /**
    * Evaluating Scala Code
    * @param code
    * @return
    */
  def evaluate(code: String): CompileResult = {

    val settings = new Settings()
    settings.embeddedDefaults[Probe]
    settings processArgumentString "-usejavacp"
    settings processArgumentString "-feature"

    val interpreter = new IMain(settings)
    println("evaluating code snippet: " + code)
    val result = try {
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val consoleByteArrayOutputStream = new ByteArrayOutputStream()
      val consoleOutputStream = new PrintStream(consoleByteArrayOutputStream)
      val ps = new PrintStream(byteArrayOutputStream);

      // Problem: Console.out just for the current thread! workaround: use out.println
      val result = Console.withOut(ps) {
        interpreter.bind("out", consoleOutputStream)
        interpreter.interpret(code)
      }

      val consoleOut = byteArrayOutputStream.toString("UTF-8") + "\n" + consoleByteArrayOutputStream.toString("UTF-8")

      val filtered = filterOutput(consoleOut)
      println(s">>> ${xml.Utility.escape(filtered)} <<<")
      println(s">>> result: ${result.toString()} <<<")
      val htmlOut = xml.Utility.escape(filtered).replaceAll("\\^", "").replaceAll("\r", "<br>").replaceAll("\n", "<br>").replaceAll("\t", " ") //.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\r", "<br>").replaceAll("\n", "<br>").replaceAll("\"", "&quot;")
      CompileResult(htmlOut, result.toString())
    } catch {
      case ex: javax.script.ScriptException =>
        println(s">>> ${ex.getMessage()} <<<")
        CompileResult(ex.getMessage().replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br>").replaceAll("\"", "&quot;"), "Error")
    } finally {
      interpreter.close
    }

    println(result)
    result
  }


  /**
    * Filter out some unwanted lines like imports, out: etc.
    * @param lines a String holding the complete source file's lines
    * @return filtered source
    */
  def filterOutput(lines: String) = {
    lines.split("\n").filter {
      line =>
        !filterOut.exists { prefix => line.trim().startsWith(prefix) }
    }.mkString("\n")
  }


  /*
   * ########################### JAVA support: ##############################
   */

  /**
   *  Read the Java class Template from resources dir:
   */
  val main = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("JavaTemplate.java")).mkString

  /**
    * Evaluating Java code by encapsulating code within a Template Class. (some Limitations)
    * Supported features:
    * imports: Lines starting with "import" are collected and grouped into the import section of the class.
    * methods: Blocks starting with "public" are collected and grouped into the methods section of the class.
    */
  def evaluateJava(code: String): CompileResult = {
    val (imports, otherLines) = code.split("\n").partition { line => line.trim().startsWith("import ") }

    val (methods, replCode) = splitMethods(otherLines.toList)

    val src = main.
      replace("// CODE_HERE", replCode.mkString("\n")).
      replace("// IMPORTS_HERE", imports.mkString("\n")).
      replace("// METHODS_HERE", methods.mkString("\n"))

    val javaFile = new StringJavaFileObject("JavaTemplate", src)

    val byteArrayOutputStream = new ByteArrayOutputStream()
    val ps = new PrintStream(byteArrayOutputStream)

    System.out.println(src)

    val outStored = System.out
    System.setOut(ps)
    val compiler = ToolProvider.getSystemJavaCompiler()
    val units = Arrays.asList(javaFile)
    val errOutputStream = new ByteArrayOutputStream()
    val err = new PrintWriter(errOutputStream)
    val errps = new PrintStream(errOutputStream)
    val errStored = System.err
    System.setErr(errps)

    val fileManager = compiler.getStandardFileManager(null, null, null)
    val result = try {
      val task = compiler.getTask(err, fileManager, null, null, null, units)
      val result = task.call()

      val classLoader = new URLClassLoader(Array(new File(".").getAbsoluteFile().toURI().toURL()))
      Class.forName("JavaTemplate", true, classLoader) // Java Compiler API 2
      result
    } finally {
      fileManager.close();
      System.setOut(outStored)
      System.setErr(errStored)
    }

    val consoleOut = byteArrayOutputStream.toString().replaceAll("\n", "<br>").replaceAll("\"", "&quot;")
    println("consoleOut: " + consoleOut)
    println("errOutputStream: " + errOutputStream.toString())
    if (result) {
      CompileResult(consoleOut, s"""Success""")
    } else {
      val errOut = errOutputStream.toString().replaceAll("\n", "<br>").replaceAll("\"", "&quot;")
      CompileResult(errOut + "<br>" + consoleOut, s"""Error""")
    }
  }

  /**
    * Find 'public' method block by balancing braces (fit's 80% of normal code)
    */
  def splitMethods(lines: List[String]): (List[String], List[String]) = {
    def findEndOfBlock(methodLines: List[String], bCount: Int, methodCollector: List[String], otherCollector: List[String]): (List[String], List[String]) = {
      methodLines match {
        case Nil => splitM(Nil, methodCollector, otherCollector)
        case l :: tail => {
          val bracketCounter = bCount + l.map {
            case '{' => 1
            case '}' => -1
            case _ => 0
          }.sum
          if (bracketCounter == 0) {
            splitM(tail, l :: methodCollector, otherCollector)
          } else {
            findEndOfBlock(tail, bracketCounter, l :: methodCollector, otherCollector)
          }
        }
      }
    }
    def splitM(lines: List[String], methodCollector: List[String], otherCollector: List[String]): (List[String], List[String]) = {
      lines match {
        case Nil => (methodCollector.reverse, otherCollector.reverse)
        case l :: tail if (l.trim.startsWith("public")) => findEndOfBlock(lines, 0, methodCollector, otherCollector)
        case l :: tail => splitM(tail, methodCollector, l :: otherCollector)
      }
    }
    splitM(lines, Nil, Nil)
  }

  class StringJavaFileObject(name: String, code: CharSequence) extends SimpleJavaFileObject(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE) {
    override def getCharContent(ignoreEncodingErrors: Boolean): CharSequence = {
      code
    }
  }

}

