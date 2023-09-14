/*
 * Copyright 2021-2022 Branko Juric, Brady Wood
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

package gwen.core.eval.binding

import gwen.core.eval.EvalContext
import gwen.core.eval.binding.DryValueBinding
import gwen.core.state.Environment

import scala.util.Try
import gwen.core.Errors

object JSFunctionBinding {
  
  def baseKey(name: String) = s"$name/${BindingType.function}"
  def jsRefKey(name: String) = s"${baseKey(name)}/jsRef"
  def argsKey(name: String) = s"${baseKey(name)}/args"
  def delimiterKey(name: String) = s"${baseKey(name)}/delimiter"

  def bind(name: String, javascriptRef: String, argsString: String, delimiter: Option[String], env: Environment): Unit = {
    env.scopes.clear(name)
    env.scopes.set(jsRefKey(name), javascriptRef)
    env.scopes.set(argsKey(name), argsString)
    delimiter foreach { delim =>
      env.scopes.set(delimiterKey(name), delim)
    }
  }

  def find[T <: EvalContext](name: String, ctx: T): Try[JSFunctionBinding[T]] = Try {
    ctx.scopes.get(jsRefKey(name))
    ctx.scopes.get(argsKey(name))
    new JSFunctionBinding(name, ctx)
  }

}

class JSFunctionBinding[T <: EvalContext](name: String, ctx: T) extends Binding[T, String](name, ctx) {

  val jsRefKey = JSFunctionBinding.jsRefKey(name)
  val argsKey = JSFunctionBinding.argsKey(name)
  val delimiterKey = JSFunctionBinding.delimiterKey(name)

  override def resolve(): String = {
    bindIfLazy(
      resolveValue(jsRefKey) { jsRef =>
        resolveValue(argsKey) { argsString =>
          if (ctx.scopes.getOpt(delimiterKey).nonEmpty) {
            resolveValue(delimiterKey) { delimiter =>
              new JSBinding(jsRef, parseArgs(jsRef, argsString.split(delimiter).toList), ctx).resolve()
            }
          } else {
            new JSBinding(jsRef, parseArgs(jsRef, List(argsString)), ctx).resolve()
          }
        }
      }
    )
  }

  private def parseArgs(jsRef: String, args: List[String]): List[String] = {
    if (!args.contains(DryValueBinding.unresolved(BindingType.javascript))) {
      val jsKey = JSBinding.key(jsRef)
      val js = ctx.scopes.get(jsKey)
      0 to (args.size - 1) foreach { idx =>
        if (!js.contains(s"arguments[$idx]")) {
          Errors.missingJSArgumentError(jsRef, idx)
        }
      }
    }
    args
  }

  override def toString: String = Try {
    resolveValue(jsRefKey) { jsRef =>
      resolveValue(argsKey) { argsString =>
        if (ctx.scopes.getOpt(delimiterKey).nonEmpty) {
          resolveValue(delimiterKey) { delimiter =>
            s"$name [${BindingType.function}: $jsRef, args: $argsString, delimiter: $delimiter]"
          }
        } else {
          s"$name [${BindingType.function}: $jsRef, arg: $argsString]"
        }
      }
    }
  } getOrElse name

}
