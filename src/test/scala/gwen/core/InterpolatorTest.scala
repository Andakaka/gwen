/*
 * Copyright 2015-2021 Branko Juric, Brady Wood
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

package gwen.core

import gwen.core.BaseTest
import gwen.core.Settings

import gwen.core.Errors
import org.scalatest.matchers.should.Matchers

class InterpolatorTest extends BaseTest with Matchers with Interpolator {

  """interpolate using property syntax: prefix "${property}"""" should "resolve" in {
    interpolateString("""hello "${property}"""") { _ => Some("you") } should be ("""hello "you"""")
  }
  
  """interpolate using property syntax: prefix ${property}""" should "resolve" in {
    interpolateString("""hello ${property}""") { _ => Some("you") } should be ("""hello you""")
  }
  
  """interpolate using property syntax: "${property}" suffix""" should "resolve" in {
    interpolateString(""""${property}" you""") { _ => Some("hello") } should be (""""hello" you""")
  }
  
  """interpolate using property syntax: ${property} "suffix"""" should "resolve" in {
    interpolateString("""${property} "you"""") { _ => Some("hello") } should be ("""hello "you"""")
  }
  
  """interpolate using property syntax: prefix ${property} suffix""" should "resolve" in {
    interpolateString("""hello ${property} good thing""") { _ => Some("you") } should be ("""hello you good thing""")
  }
  
  """interpolate nested using property syntax: ${property1-${property0}}"""" should "resolve" in {
    interpolateString("""Go you ${property-${id}} thing!""") {
      case "id" => Some("0")
      case "property-0" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }
  
  """interpolate adjacent values using property syntax: ${property-0} ${property-1}"""" should "resolve" in {
    interpolateString("""Go you ${property-0} ${property-1} thing!""") {
      case "property-0" => Some("really")
      case "property-1" => Some("good")
      case _ => None
    } should be ("""Go you really good thing!""")
  }
  
  """interpolate adjacent values using property syntax (no space): ${property-0}${property-1}"""" should "resolve" in {
    interpolateString("""Go you ${property-0}${property-1} thing!""") {
      case "property-0" => Some("go")
      case "property-1" => Some("od")
      case _ => None
    } should be ("""Go you good thing!""")
  }
  
  """interpolate using stepdef param syntax: prefix "$<param>"""" should "resolve" in {
    interpolateString("""hello "$<param>"""") { _ => Some("you") } should be ("""hello "you"""")
  }
  
  """interpolate using stepdef param syntax: prefix $<param>""" should "resolve" in {
    interpolateString("""hello $<param>""") { _ => Some("you") } should be ("""hello you""")
  }
  
  """interpolate using stepdef param syntax: "$<param>" suffix""" should "resolve" in {
    interpolateString(""""$<param>" you""") { _ => Some("hello") } should be (""""hello" you""")
  }
  
  """interpolate using stepdef param syntax: $<param> "suffix"""" should "resolve" in {
    interpolateString("""$<param> "you"""") { _ => Some("hello") } should be ("""hello "you"""")
  }
  
  """interpolate using stepdef param syntax: prefix $<param> suffix""" should "resolve" in {
    interpolateString("""hello $<param> good thing""") { _ => Some("you") } should be ("""hello you good thing""")
  }
  
  """interpolate nested using stepdef param syntax: $<param1-$<param0>>"""" should "resolve" in {
    interpolateString("""Go you $<param-$<id>> thing!""") {
      case "<id>" => Some("0")
      case "<param-0>" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }
  
  """interpolate stepdef with adjacent params: $<param-0> $<param-1>"""" should "resolve" in {
    interpolateString("""Go you $<param-0> $<param-1> thing!""") {
      case "<param-0>" => Some("really")
      case "<param-1>" => Some("good")
      case _ => None
    } should be ("""Go you really good thing!""")
  }
  
  """interpolate stepdef with adjacent params (no space): $<param-0>$<param-1>"""" should "resolve" in {
    interpolateString("""Go you $<param-0>$<param-1> thing!""") {
      case "<param-0>" => Some("go")
      case "<param-1>" => Some("od")
      case _ => None
    } should be ("""Go you good thing!""")
  }
  
  """interpolating stepdef in dry run mode: $<param-0>$<param-1>"""" should "decorate parameters" in {
    interpolateString("""Go you $<param-0>$<param-1> thing!""") {
      case _ => None
    } should be ("""Go you $[param:param-0]$[param:param-1] thing!""")
  }
  
  """embedded + literal in string""" should "not be treated as a concatenation operator" in {
    interpolateString("""I enter "+6149265587" in the phone field""") { _ => throw new Exception("should not throw this") } should be ("""I enter "+6149265587" in the phone field""")
  }

  """interpolate env var using property syntax: prefix "${property}"""" should "resolve" in {
    interpolateString("""home "${env.HOME}"""") { _ => Settings.getOpt("env.HOME") } should be (s"""home "${sys.env("HOME")}"""")
  }
  
  """interpolate env var using property syntax: prefix ${property}""" should "resolve" in {
    interpolateString("""home ${env.HOME}""") { _ => Settings.getOpt("env.HOME") } should be (s"""home ${sys.env("HOME")}""")
  }
  
  """interpolate env var using property syntax: "${property}" suffix""" should "resolve" in {
    interpolateString(""""${env.HOME}" home""") { _ => Settings.getOpt("env.HOME") } should be (s""""${sys.env("HOME")}" home""")
  }
  
  """interpolate env var using property syntax: ${property} "suffix"""" should "resolve" in {
    interpolateString("""${property} "you"""") { _ => Some("hello") } should be ("""hello "you"""")
    interpolateString("""${env.HOME} home""") { _ => Settings.getOpt("env.HOME") } should be (s"""${sys.env("HOME")} home""")
  }
  
  """interpolate env var using property syntax: prefix ${property} suffix""" should "resolve" in {
    interpolateString("""hello ${property} good thing""") { _ => Some("you") } should be ("""hello you good thing""")
    interpolateString("""home ${env.HOME} found""") { _ => Settings.getOpt("env.HOME") } should be (s"""home ${sys.env("HOME")} found""")
  }
  
  """interpolate nested env var using property syntax: ${property1-${property0}}"""" should "resolve" in {
    interpolateString("""Go you ${env.var-${env.id}} thing!""") {
      case "env.id" => Some("0")
      case "env.var-0" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }
  
  """interpolate adjacent env var values using property syntax: ${property-0} ${property-1}"""" should "resolve" in {
    interpolateString("""Go you ${env.var0} ${env.var1} thing!""") {
      case "env.var0" => Some("really")
      case "env.var1" => Some("good")
      case _ => None
    } should be ("""Go you really good thing!""")
  }
  
  """interpolate adjacent env var values using property syntax (no space): ${property-0}${property-1}"""" should "resolve" in {
    interpolateString("""Go you ${env.var0}${env.var1} thing!""") {
      case "env.var0" => Some("go")
      case "env.var1" => Some("od")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """multi line string with properties""" should "resolve" in {
    val source =
      """hello
        |${property}""".stripMargin
    val target =
      """hello
        |you""".stripMargin
    interpolateString(source) { _ => Some("you") } should be(target)
  }

  """multi line string with env vars""" should "resolve" in {
    val source =
      """hello
        |${env.var}""".stripMargin
    val target =
      """hello
        |you""".stripMargin
    interpolateString(source) { _ => Some("you") } should be(target)
  }

  """Nested parameter in property: ${property-$<param>}"""" should "resolve" in {
    interpolateString("""Go you ${property-$<id>} thing!""") {
      case "<id>" => Some("0")
      case "property-0" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Nested parameter in env var: ${env.var_$<param>}"""" should "resolve" in {
    interpolateString("""Go you ${env.var_$<id>} thing!""") {
      case "<id>" => Some("0")
      case "env.var_0" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Nested property in parameter: $<param-${property}>"""" should "resolve" in {
    interpolateString("""Go you $<param-${id}> thing!""") {
      case "id" => Some("0")
      case "<param-0>" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Nested env var in parameter: $<param-${env.var}>"""" should "resolve" in {
    interpolateString("""Go you $<param-${env.var}> thing!""") {
      case "env.var" => Some("0")
      case "<param-0>" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Interpolation of Params""" should "resolve 1 available param" in {
    interpolateParams("""Go you ${env.var0} $<param> thing!""") {
      case "<param>" => Some("good")
      case x => Errors.unboundAttributeError(x, "local")
    } should be ("""Go you ${env.var0} good thing!""")
  }

  """Interpolation of Params""" should "resolve 2 available params" in {
    interpolateParams("""Go you $<param1> ${env.var0} thing $<param2>!""") {
      case "<param1>" => Some("good")
      case "<param2>" => Some("you")
      case x => Errors.unboundAttributeError(x, "local")
    } should be ("""Go you good ${env.var0} thing you!""")
  }

  """Interpolation of Params""" should "resolve 2 available params and skip missing param" in {
    interpolateParams("""Go you $<param1> $<param2> thing $<param3>!""") {
      case "<param1>" => Some("good")
      case "<param3>" => Some("you")
      case x => Errors.unboundAttributeError(x, "local")
    } should be ("""Go you good $<param2> thing you!""")
  }

  """Interpolation of Params""" should "resolve 2 available params and skip composite param" in {
    interpolateParams("""Go you $<param1> $<${env.var0}> thing $<param2>!""") {
      case "<param1>" => Some("good")
      case "<param2>" => Some("you")
      case x => Errors.unboundAttributeError(x, "local")
    } should be ("""Go you good $<${env.var0}> thing you!""")
  }

  """Same named property and parameter: $<name> ${name}"""" should "resolve" in {
    interpolateString("""Go you $<name> ${name}!""") {
      case "name" => Some("thing")
      case "<name>" => Some("good")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Same named parameter and property: ${name} $<name>"""" should "resolve" in {
    interpolateString("""Go you ${name} $<name>!""") {
      case "name" => Some("good")
      case "<name>" => Some("thing")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Nested parameter that resolves to same named property: $<${name}>"""" should "resolve" in {
    interpolateString("""Go you $<${name}>!""") {
      case "name" => Some("name")
      case "<name>" => Some("good thing")
      case _ => None
    } should be ("""Go you good thing!""")
  }

  """Nested property that resolves to same named parameter: ${$<name>}"""" should "resolve" in {
    interpolateString("""Go you ${$<name>}!""") {
      case "name" => Some("good thing")
      case "<name>" => Some("name")
      case _ => None
    } should be ("""Go you good thing!""")
  }

}
