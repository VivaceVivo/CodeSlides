package de.ptr.codecentered

import collection.mutable.Stack
import org.scalatest._


class CompilerTests extends FlatSpec with Matchers {

  "A ScriptCompiler" should "execute simple scala code" in {
    
	  val compiler = new ScriptCompiler()
	  val result = compiler.evaluate(
	  """|object HalloWelt {
		 |  def main(args: Array[String]) {
		 |    println("Hallo, Welt!")
		 |  }
		 |}
	     |HalloWelt.main(Array[String]())
	     """)
	     result should be ("5")

  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    } 
  }
}