/*
 * Copyright 2014-2021 Branko Juric, Brady Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gwen

import gwen.core._
import gwen.core.eval.EvalContext
import gwen.core.eval.EvalEngine
import gwen.core.eval.GwenLauncher
import gwen.core.eval.GwenREPL
import gwen.core.init.NoopProjectInitialiser
import gwen.core.node.gherkin.Dialect
import gwen.core.state.EnvState
import gwen.core.status.Failed

import com.typesafe.scalalogging.LazyLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.fusesource.jansi.AnsiConsole
import org.slf4j.bridge.SLF4JBridgeHandler

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URL
import java.util.{ logging => jul}

/**
  * Default Gwen interpreter application.
  */
object DefaultGwenInterpreter extends GwenInterpreter(EvalEngine())

object GwenInterpreter {
  def apply() = new GwenInterpreter(EvalEngine())
}

/**
  * Main Gwen application superclass.
  *
  * @param engine the evaluation engine
  */
class GwenInterpreter[T <: EvalContext](engine: EvalEngine[T]) extends GwenLauncher(engine) with LazyLogging with NoopProjectInitialiser {

  def main(args: Array[String]): Unit = {
    printBanner("Welcome to ")
    val start = System.nanoTime
    try {
      initDefaultEnvSettings()
      val options = GwenOptions(args)
      logger.info("Initialising settings")
      Settings.init(options.settingsFiles*)
      GwenSettings.check()
      initLogging(options)
      Dialect.instance
      System.exit(run(options))
    } catch {
      case e: Throwable =>
        val errMsg = s"${Failed(System.nanoTime - start, e).message}"
        if (e.isInstanceOf[Errors.GwenException]) {
          logger.error(errMsg)
        } else {
          logger.error(errMsg, e)
        }
        println()
        System.exit(1)
    }
  }

  def initDefaultEnvSettings(): Unit = {
    applyEnvSettings(
      List(
        ("gwen.cli.options.dryRun", "GWEN_DRY_RUN"), 
        ("gwen.cli.options.parallel", "GWEN_PARALLEL")
      )
    )
  }

  def applyEnvSettings(defaultEnvSettings: List[(String, String)]) = {
    defaultEnvSettings foreach { (name, envVar) =>
      sys.env.get(envVar) foreach { value => 
        if (sys.props.get(name).isEmpty) {
          sys.props.put(name, value)
        }
      }
    }
  }

  /**
    * Runs the interpreter with the given options
    *
    * @param options the command line options
    * @param launcher Gwen launcher
    * @return 0 if successful; 1 otherwise
    */
  private [gwen] def run(options: GwenOptions): Int = {
    val ctxOpt = if (options.batch || options.init) None else Some(engine.init(options, EnvState()))
    try {
      val evalStatus = run(options, ctxOpt)
      if (!options.init) {
        ctxOpt foreach { ctx =>
          if (evalStatus.isEvaluated || evalStatus.isLoaded) {
            printBanner("")
          }
          createRepl(ctx).run()
        }
      }
      evalStatus.exitCode
    } finally {
      ctxOpt.foreach(_.close())
    }
  }

  private def initLogging(options: GwenOptions): Unit = {

    if (ConsoleColors.isEnabled) {
      AnsiConsole.systemInstall()
    }

    if (options.verbose) {
      val config = getClass.getResource("/log4j2-verbose.properties")
      val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
      context.setConfigLocation(config.toURI)
    } else {
      
      // suppress error stream
      System.setErr(new PrintStream(
        new OutputStream() {
          override def write(b: Int): Unit = { }
        }
      ))

      // send all j.u.l logs to slf4j
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()

    }

    // load user provided lo4j configuration
    Settings.getOpt("log4j2.configurationFile")
      .orElse(Settings.getOpt("log4j2.configuration"))
      .orElse(Settings.getOpt("log4j.configurationFile"))
      .orElse(Settings.getOpt("log4j.configuration")).foreach { config =>

      val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
      if (config.toLowerCase.trim startsWith "file:") {
        context.setConfigLocation(new URL(config).toURI)
      } else {
        context.setConfigLocation(new File(config).toURI)
      }

    }

  }

  /**
    * Returns the console REPL.
    *
    * @param ctx the evaluation context
    */
  private [gwen] def createRepl(ctx: T): GwenREPL[T] = new GwenREPL[T](engine, ctx)

  private def printBanner(intro: String): Unit = {
    println(
      """|
         |   __ ___      _____ _ __     _    
         |  / _` \ \ /\ / / _ \ '_ \   { \," 
         | | (_| |\ V  V /  __/ | | | {_`/   
         |  \__, | \_/\_/ \___|_| |_|   `    
         |  |___/                            
         |
             |""".stripMargin + intro + engine.implName + " v" + engine.implVersion + engine.noticeMsg.map(msg => s"${System.lineSeparator}$msg").getOrElse("") + """|
         |gweninterpreter.org
         |""".stripMargin
      )

    sys.env.get("GWEN_WEB_HOME").filter(_.nonEmpty) foreach { _ =>
      println(
        """|
           | ╭──────────────────────────────────────────────────────────╮
           | │  Gwen Workspaces DEPRECATED!                             │
           | │                                                          │
           | │  Gwen Workspaces are deprecated in favor of JS projects  │
           | │  in Gwen 3. Please visit the migration page for options  │
           | │  at https://gweninterpreter.org/docs/migration/gwen3     │
           | ╰──────────────────────────────────────────────────────────╯""".stripMargin
      )
    }
  }

}
