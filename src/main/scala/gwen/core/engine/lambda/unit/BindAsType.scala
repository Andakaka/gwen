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

package gwen.core.engine.lambda.unit

import gwen.core._
import gwen.core.engine.EvalContext
import gwen.core.engine.lambda.UnitStep
import gwen.core.model.BehaviorType
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step
import gwen.core.engine.binding.BindingType
import gwen.core.engine.binding.FileBinding
import gwen.core.engine.binding.JavaScriptBinding
import gwen.core.engine.binding.SysprocBinding

class BindAsType[T <: EvalContext](target: String, bindingType: BindingType.Value, value: String) extends UnitStep[T] {

  override def apply(parent: GwenNode, step: Step, ctx: T): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Context, ctx)
      bindingType match {
        case BindingType.javascript => JavaScriptBinding.bind(target, value, ctx)
        case BindingType.sysproc => SysprocBinding.bind(target, value, ctx)
        case BindingType.file => FileBinding.bind(target, value, ctx)
        case _ => ctx.topScope.set(target, Settings.get(value))
      }
    }
  }

}

