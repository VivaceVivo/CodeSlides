
package de.ptr.codecentered

import akka.http.scaladsl.server.Directives._
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.StatusCodes.MovedPermanently
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext
import spray.json.DefaultJsonProtocol

import java.io.File
import java.io.FilenameFilter

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.Uri.apply
import akka.http.scaladsl.server.Directive.addByNameNullaryApply
import akka.http.scaladsl.server.Directive.addDirectiveApply

trait Protocol extends DefaultJsonProtocol

trait BaseService extends Protocol with SprayJsonSupport with Config {
  protected implicit def executor: ExecutionContext
  protected implicit def materializer: ActorMaterializer
  protected def log: LoggingAdapter
}

/**
 * AKKA HTTP backend for the Slides & Scripting services.
 */
trait SlideService extends BaseService {

  val eval = path("eval") {
    post {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
        entity(as[String]) { code =>
          println(s"<code>\n$code\n</code>")
          val executor = new ScriptCompiler
          try {
            val result = executor.evaluate(code)
            complete {
              s"""{"result" : "${result.result}", "console" : "${result.console}"}"""
            }
          } catch {
            case ex: javax.script.ScriptException => complete { "ERROR" }
          }
        }
      }
    }
  }

  val evalJava = path("evaljava") {
    post {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
        entity(as[String]) { code =>
          println(s"<code>\n$code\n</code>")
          val executor = new ScriptCompiler
          try {
            val result = executor.evaluateJava(code)
            complete {
              s"""{"result" : "${result.result}", "console" : "${result.console}"}"""
            }
          } catch {
            case ex: javax.script.ScriptException => complete { "ERROR" }
          }
        }
      }
    }
  }

  val default = get {
    redirect("/slides", MovedPermanently)
  }

  val list = pathPrefix("slides") { get { getFromResourceDirectory("slides") } }

  val slides = path("slides") {
    get {
      complete {
        <div>
          <h1>All Slides</h1>
          <ul>
            { findSlides.map(name => <li><a href={ "slides/" + name }>{ name }</a></li>) }
          </ul>
        </div>
      }
    }
  }

  def findSlides = {
    val basedir = new File(".");
    val dir = new File(basedir.getAbsolutePath() + "/src/main/resources/slides")
    dir.list(new FilenameFilter() {
      override def accept(dir: File, name: String) = name.toLowerCase().endsWith(".html")
    }).toList
  }

  val routes = list ~ slides ~ eval ~ evalJava ~ default
}

