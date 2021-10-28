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

package gwen.core.result

import gwen.core.BaseTest
import gwen.core.TestModel
import gwen.core.node.gherkin._
import gwen.core.status._

import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration

import java.util.Date

class ResultsSummaryTest extends BaseTest with Matchers with TestModel {
  
  val OK1 = OK(1000000)
  val OK2 = OK(2000000)
  val OK3 = OK(3000000)
  val OK4 = OK(4000000)
  
  val Failed3 = Failed(3000000, new Exception())
  val Failed4 = Failed(4000000, new Exception())

  "No results in summary" should "yield empty metrics" in {
    val summary = ResultsSummary(Duration.Zero)
    summary.results.size should be (0)
    summary.featureCounts.size should be (0)
    summary.scenarioCounts.size should be (0)
    summary.stepCounts.size should be (0)
    val summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("0 features: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("0 scenarios: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(3) should be ("0 steps: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summary.statusString.contains("OK") should be (true)
  }
  
  "Accumulated feature results in summary" should "sum correctly" in {
    
    var summary = ResultsSummary(Duration.Zero)
    var summaryLines = Array[String]()
    
    // add 1 meta
    val meta1 = Spec(
      Feature(None, "meta1", Nil), None, List(
        Scenario(List[Tag](), "metaScenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "meta step 1", OK2),
          Step(StepKeyword.When.toString, "meta step 2", OK1),
          Step(StepKeyword.Then.toString, "meta step 3", OK2))
        ),
        Scenario(List(Tag("@StepDef")), "metaStepDef1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", Loaded),
          Step(StepKeyword.When.toString, "step 2", Loaded),
          Step(StepKeyword.Then.toString, "step 3", Loaded))
        )), Nil, Nil)
        
    // add 1 OK scenario
    val feature1 = Spec(
      Feature(None, "feature1", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK1),
          Step(StepKeyword.Then.toString, "step 3", OK2))
        ),
        Scenario(List(Tag("@StepDef")), "StepDef1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", Loaded),
          Step(StepKeyword.When.toString, "step 2", Loaded),
          Step(StepKeyword.Then.toString, "step 3", Loaded))
        )),
      Nil,
      List(meta1))
        
    val metaResult = new SpecResult(meta1, None, Nil, new Date(), new Date())
    var featureResult = new SpecResult(feature1, None, List(metaResult), new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.OK)
    summary.results.size should be (1)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 3)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("1 feature: OK 1, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("1 scenario: OK 1, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(3) should be ("3 steps: OK 3, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summary.statusString.contains("OK") should be (true)
    
    // add 1 failed scenario
    val feature2 = Spec(
      Feature(None, "feature2", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", Failed3),
          Step(StepKeyword.Then.toString, "step 3", Skipped))
        )), Nil, Nil)
    featureResult = new SpecResult(feature2, None, Nil, new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.Failed)
    summary.results.size should be (2)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 1), (StatusKeyword.Failed -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 1), (StatusKeyword.Failed -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 4), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("2 features: OK 1, Failed 1, Sustained 0, Skipped 0, Pending 0")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("2 scenarios: OK 1, Failed 1, Sustained 0, Skipped 0, Pending 0")
    summaryLines(3) should be ("6 steps: OK 4, Failed 1, Sustained 0, Skipped 1, Pending 0")
    summary.statusString.contains("Failed") should be (true)
    
    // add 2 OK scenarios
    val feature3 = Spec(
      Feature(None, "feature3", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK1),
          Step(StepKeyword.Then.toString, "step 3", OK2))
        ), 
        Scenario(List[Tag](), "scenario2", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK1),
          Step(StepKeyword.Then.toString, "step 3", OK2))
        )), Nil, Nil)
    featureResult = new SpecResult(feature3, None, Nil, new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.Failed)
    summary.results.size should be (3)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 2), (StatusKeyword.Failed -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 3), (StatusKeyword.Failed -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 10), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("3 features: OK 2, Failed 1, Sustained 0, Skipped 0, Pending 0")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("4 scenarios: OK 3, Failed 1, Sustained 0, Skipped 0, Pending 0")
    summaryLines(3) should be ("12 steps: OK 10, Failed 1, Sustained 0, Skipped 1, Pending 0")
    summary.statusString.contains("Failed") should be (true)
    
    // add 1 skipped scenario
    val feature4 = Spec(
      Feature(None, "feature4", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", Skipped),
          Step(StepKeyword.When.toString, "step 2", Skipped),
          Step(StepKeyword.Then.toString, "step 3", Skipped))
        )), Nil, Nil)
    featureResult = new SpecResult(feature4, None, Nil, new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.Failed)
    summary.results.size should be (4)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 2), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 3), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 10), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 4)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("4 features: OK 2, Failed 1, Sustained 0, Skipped 1, Pending 0")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("5 scenarios: OK 3, Failed 1, Sustained 0, Skipped 1, Pending 0")
    summaryLines(3) should be ("15 steps: OK 10, Failed 1, Sustained 0, Skipped 4, Pending 0")
    summary.statusString.contains("Failed") should be (true)
    
    // add 1 pending scenario
    val feature5 = Spec(
      Feature(None, "feature5", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", Pending),
          Step(StepKeyword.When.toString, "step 2", Pending))
        )), Nil, Nil)
    featureResult = new SpecResult(feature5, None, Nil, new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.Failed)
    summary.results.size should be (5)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 2), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1), (StatusKeyword.Pending -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 3), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 1), (StatusKeyword.Pending -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 10), (StatusKeyword.Failed -> 1), (StatusKeyword.Skipped -> 4), (StatusKeyword.Pending -> 2)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("5 features: OK 2, Failed 1, Sustained 0, Skipped 1, Pending 1")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("6 scenarios: OK 3, Failed 1, Sustained 0, Skipped 1, Pending 1")
    summaryLines(3) should be ("17 steps: OK 10, Failed 1, Sustained 0, Skipped 4, Pending 2")
    summary.statusString.contains("Failed") should be (true)
    
    // add 4 OK and 1 failed scenario
    val feature6 = Spec(
      Feature(None, "feature6", Nil), None, List(
        Scenario(List[Tag](), "scenario1", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK1),
          Step(StepKeyword.Then.toString, "step 3", OK2))
        ), 
        Scenario(List[Tag](), "scenario2", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK4),
          Step(StepKeyword.When.toString, "step 2", OK1))
        ),
        Scenario(List[Tag](), "scenario3", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK1),
          Step(StepKeyword.Then.toString, "step 3", OK2))
        ),
        Scenario(List[Tag](), "scenario4", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK2),
          Step(StepKeyword.When.toString, "step 2", OK3))
        ),
        Scenario(List[Tag](), "scenario5", Nil, None, List(
          Step(StepKeyword.Given.toString, "step 1", OK1),
          Step(StepKeyword.When.toString, "step 2", Failed4),
          Step(StepKeyword.Then.toString, "step 3", Skipped),
          Step(StepKeyword.And.toString, "step 3", Skipped))
        )), Nil, Nil)
    featureResult = new SpecResult(feature6, None, Nil, new Date(), new Date())
    summary = summary + featureResult
    EvalStatus(summary.results.map(_.spec.evalStatus)).keyword should be (StatusKeyword.Failed)
    summary.results.size should be (6)
    summary.featureCounts should equal (Map((StatusKeyword.OK -> 2), (StatusKeyword.Failed -> 2), (StatusKeyword.Skipped -> 1), (StatusKeyword.Pending -> 1)))
    summary.scenarioCounts should equal (Map((StatusKeyword.OK -> 7), (StatusKeyword.Failed -> 2), (StatusKeyword.Skipped -> 1), (StatusKeyword.Pending -> 1)))
    summary.stepCounts should equal (Map((StatusKeyword.OK -> 21), (StatusKeyword.Failed -> 2), (StatusKeyword.Skipped -> 6), (StatusKeyword.Pending -> 2)))
    summaryLines = summary.statsString.split("\\r?\\n");
    summaryLines.size should be (4)
    summaryLines(0) should be ("6 features: OK 2, Failed 2, Sustained 0, Skipped 1, Pending 1")
    summaryLines(1) should be ("0 rules: OK 0, Failed 0, Sustained 0, Skipped 0, Pending 0")
    summaryLines(2) should be ("11 scenarios: OK 7, Failed 2, Sustained 0, Skipped 1, Pending 1")
    summaryLines(3) should be ("31 steps: OK 21, Failed 2, Sustained 0, Skipped 6, Pending 2")
    summary.statusString.contains("Failed") should be (true)
    
  }
  
}