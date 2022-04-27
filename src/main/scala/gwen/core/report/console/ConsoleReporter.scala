/*
 * Copyright 2021 Branko Juric, Brady Wood
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

package gwen.core.report.console

import gwen._
import gwen.core._
import gwen.core.node._
import gwen.core.node.gherkin._
import gwen.core.node.event.NodeEvent
import gwen.core.node.event.NodeEventListener
import gwen.core.result.SpecResult

import gwen.core.status.StatusKeyword
import gwen.core.result.ResultsSummary

class ConsoleReporter(options: GwenOptions)
    extends NodeEventListener("Console Reporter", Set(NodeType.Meta, NodeType.StepDef)) {

  private val parallel = options.parallel || options.parallelFeatures
  private val printer = new SpecPrinter(parallel, ConsoleColors.isEnabled)

  override def beforeUnit(event: NodeEvent[FeatureUnit]): Unit = { 
    val unit = event.source
    val action = if (options.dryRun) "Checking" else "Executing"
    if (parallel) {
      System.out.println(s"""[""" + Thread.currentThread.getName + "] " + action + " " + SpecType.Feature.toString.toLowerCase + " specification: " + unit.name + """
                             |""".stripMargin)
    } else {
      System.out.println(("""|   _
                             |  { \," """ + action + " " + SpecType.Feature.toString.toLowerCase + """:
                             | {_`/   """ + unit.name + """
                             |    `
                             |""").stripMargin)
    }
  } 

  override def afterUnit(event: NodeEvent[FeatureUnit]): Unit = {
    if (!parallel) {
      System.out.println()
    } else {
      val unit = event.source
      val parent = event.callChain.previous
      val action = if (options.dryRun) "Checked" else "Executed"
      unit.result foreach { result =>
        System.out.println(
          ("""|   _
              |  { \," [""" + Thread.currentThread.getName + "] " + action + " " + SpecType.Feature.toString.toLowerCase + """ specification:
              | {_`/   """ + unit.name + """
              |    `
              |
              |""").stripMargin + printer.prettyPrint(parent, result.spec) + printer.printSpecResult(result)
        )
      }
    }
  }
    
  override def beforeSpec(event: NodeEvent[Spec]): Unit = {
    if (!parallel) {
      val spec = event.source
      val parent = event.callChain.previous
      System.out.println(printer.prettyPrint(parent, spec.feature))
    }
  }

  override def afterSpec(event: NodeEvent[SpecResult]): Unit = {
    if (!parallel) {
      val result = event.source
      System.out.print(printer.printSpecResult(result))
    }
  }

  override def beforeBackground(event: NodeEvent[Background]): Unit = {
    if (!parallel) {
      val background = event.source
      val parent = event.callChain.previous
      System.out.println(printer.prettyPrint(parent, background))
    }
  }

  override def afterBackground(event: NodeEvent[Background]): Unit = {
    if (!parallel) {
      event.callChain.nodes.reverse.find { node => 
        node.isInstanceOf[Scenario]
      } map { node => 
        node.asInstanceOf[Scenario]
      } foreach { scenario =>
        val parent = event.callChain.previous
        System.out.println(printer.prettyPrint(parent, scenario))
      }
    }
  }

  override def beforeScenario(event: NodeEvent[Scenario]): Unit = {
    if (!parallel) {
      val scenario = event.source
      if (scenario.background.isEmpty) {
        val parent = event.callChain.previous
        System.out.println(printer.prettyPrint(parent, scenario))
      }
    }
  }

  override def afterScenario(event: NodeEvent[Scenario]): Unit = {  }

  override def beforeExamples(event: NodeEvent[Examples]): Unit = {
    if (!parallel) {
      val examples = event.source
      val parent = event.callChain.previous
      System.out.println(printer.prettyPrint(parent, examples))
    }
  }

  override def afterExamples(event: NodeEvent[Examples]): Unit = {  }

  override def beforeRule(event: NodeEvent[Rule]): Unit = {
    if (!parallel) {
      val rule = event.source
      val parent = event.callChain.previous
      System.out.println(printer.prettyPrint(parent, rule))
    }
  }

  override def afterRule(event: NodeEvent[Rule]): Unit = {  }

  override def beforeStepDef(event: NodeEvent[Scenario]): Unit = { }
  override def afterStepDef(event: NodeEvent[Scenario]): Unit = { }

  override def beforeStep(event: NodeEvent[Step]): Unit = {
    if (!parallel) {
      val step = event.source
      val parent = event.callChain.previous
      if (!parent.isInstanceOf[Step]) {
        System.out.print(printer.prettyPrint(parent, step))
      }
    }
  }

  override def afterStep(event: NodeEvent[Step]): Unit = {
    if (!parallel) {
      val step = event.source
      val parent = event.callChain.previous
      if (!parent.isInstanceOf[Step]) {
        System.out.println(printer.printStatus(step, withMessage = true))
      }
    }
  }

  def printSummary(summary: ResultsSummary): Unit = {
    if (summary.results.size > 1) {
      System.out.println(printer.printSummary(summary))
    }
    val reports = summary.reports
    if (reports.nonEmpty) {
      val maxWidh = (reports map { (format, _) => format.toString.length }).max
      reports foreach { (format, report) => 
        System.out.println(s"${Formatting.leftPad(s"${format.toString.toUpperCase} report", maxWidh + 7)}  $report")
      }
      System.out.println()
    }
  }
  
}
