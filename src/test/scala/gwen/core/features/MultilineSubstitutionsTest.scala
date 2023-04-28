/*
 * Copyright 2016-2021 Branko Juric, Brady Wood
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
package gwen.core.features

import gwen.GwenInterpreter
import gwen.core.BaseTest
import gwen.core.GwenOptions
import gwen.core.Settings
import gwen.core.report.ReportFormat
import gwen.core.status._

import org.scalatest.prop.TableDrivenPropertyChecks.forAll

import java.io.File

class MultilineSubstitutionsTest extends BaseTest {

  val interpreter = GwenInterpreter()

  forAll (levels) { level =>
    s"multiline substitution features using $level level state" should "evaluate without error" in {
      withSetting("gwen.state.level", level) {
        val options = GwenOptions(
          batch = true,
          reportDir = Some(new File(s"target/report/multiline/$level-level")), 
          reportFormats = List(ReportFormat.html, ReportFormat.junit, ReportFormat.json),
          features = List(new File("src/test/features/multiline"))
        )
          
        Settings.init(options.settingsFiles*)
        interpreter.run(options, None) match {
          case _: Passed => // excellent :)
          case Failed(_, error) => error.printStackTrace(); fail(error.getMessage)
          case _ => fail("evaluation expected but got noop")
        }
      }
    }
  }
  
  forAll (levels) { level =>
    s"multiline substitution using $level level state" should "pass --dry-run test" in {  
      withSetting("gwen.state.level", level) {
        val options = GwenOptions(
          batch = true,
          reportDir = Some(new File(s"target/report/multiline-dry-run/$level-level")), 
          reportFormats = List(ReportFormat.html, ReportFormat.junit, ReportFormat.json),
          features = List(new File("src/test/features/multiline")),
          dryRun = true
        )
        
        Settings.init(options.settingsFiles*)
        interpreter.run(options, None) match {
          case _: Passed => // excellent :)
          case Failed(_, error) => error.printStackTrace(); fail(error.getMessage)
          case _ => fail("evaluation expected but got noop")
        }
      }
    }
  }
  
}