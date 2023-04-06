/*
 * Copyright 2022 Branko Juric, Brady Wood
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

package gwen.core.eval.lambda.composite

import gwen.core.Errors
import gwen.core.eval.EvalContext
import gwen.core.eval.binding.LoadStrategyBinding
import gwen.core.eval.lambda.CompositeStep
import gwen.core.eval.engine.StepDefEngine
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Annotations
import gwen.core.node.gherkin.Scenario
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.Tag
import gwen.core.status._

import util.chaining.scalaUtilChainingOps

import scala.util.Try

class IfDefinedCondition[T <: EvalContext](doStep: String, name: String, negate: Boolean, engine: StepDefEngine[T]) extends CompositeStep[T](doStep) {

  override def apply(parent: GwenNode, step: Step, ctx: T): Step = {
    def cond = s"$name is ${if (negate) "not " else ""}defined"
    ctx.getStepDef(doStep, None) foreach { stepDef =>
      checkStepDefRules(step.copy(withName = doStep, withStepDef = Some(stepDef)), ctx)
    }
    val iStep = step.copy(withEvalStatus = Pending)
    val ifTag = Tag(Annotations.If)
    val tags = List(Tag(Annotations.Synthetic), ifTag, Tag(Annotations.StepDef))
    val iStepDef = Scenario(None, tags, ifTag.toString, cond, Nil, None, List(step.copy(withName = doStep)), Nil, Nil, Nil)
    val sdCall = () => engine.callStepDef(step, iStepDef, iStep, ctx)
    ctx.evaluate(sdCall()) {
      val satisfied = {
        val result = Try(ctx.getBinding(name))
        if (!negate) result.isSuccess else result.isFailure
      }
      LoadStrategyBinding.bindIfLazy(name, satisfied.toString, ctx)
      if (satisfied) {
        logger.info(s"Processing conditional step ($cond = true): ${step.keyword} $doStep")
        sdCall()
      } else {
        logger.info(s"Skipping conditional step ($cond = false): ${step.keyword} $doStep")
        step.copy(withEvalStatus = Passed(0, abstained = !ctx.options.dryRun))
      }
    }
  }

}
