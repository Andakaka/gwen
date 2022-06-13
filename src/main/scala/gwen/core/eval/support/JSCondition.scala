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

package gwen.core.eval.support

import gwen.core.eval.EvalContext
import gwen.core.eval.binding.Binding
import gwen.core.eval.binding.JSBinding
import gwen.core.eval.binding.JSFunctionBinding

import scala.util.Failure
import scala.util.Success
import scala.util.Try


class JSCondition[T <: EvalContext](condition: String, negate: Boolean, ctx: T) {
  
  val (name, binding, negated) = {
    if (negate) {
      find(s"not $condition") match {
        case Success(b) => (s"not $condition", b, false)
        case Failure(e) => (condition, find(condition) getOrElse { throw e }, true)
      }
    } else {
      find(condition) match {
        case Success(b) => (condition, b, false)
        case Failure(e) => throw e
      }
    }
  }
    
  private def find (name: String): Try[Binding[T, String]] = Try {
    JSBinding.find(name, ctx) match {
      case Success(binding) => 
        binding
      case Failure(e) => 
        JSFunctionBinding.find(name, ctx) getOrElse { throw e }
    }
  }

  def evaluate(): Boolean = {
    val result = binding.resolve().toBoolean
    if (negated) !result else result
  }

}