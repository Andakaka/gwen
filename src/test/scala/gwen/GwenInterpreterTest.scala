/*
 * Copyright 2014 Branko Juric, Brady Wood
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

import gwen.core.GwenOptions
import gwen.core.engine.EvalContext
import gwen.core.engine.EvalEngine
import gwen.core.model.Skipped
import gwen.core.model.Passed
import gwen.core.model.state.EnvState

import org.mockito.Matchers.any
import org.mockito.Matchers.same
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.File

class GwenInterpreterTest extends FlatSpec with Matchers with MockitoSugar {

  private def createApp(options: GwenOptions, engine: EvalEngine[EvalContext], repl: GwenREPL[EvalContext]) = {
    new GwenInterpreter(engine) {
      override private [gwen] def createRepl(ctx: EvalContext): GwenREPL[EvalContext] = repl
    }
  }
  
  "Running app with no args" should "initialise env, execute options, run repl, and close env" in {
    
    val options = GwenOptions()
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockCtx = spy(new EvalContext(options, EnvState()))
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = Mockito.mock(classOf[EvalEngine[EvalContext]], Mockito.CALLS_REAL_METHODS)
    val app = createApp(options, mockEngine, mockRepl)

    when(mockEngine.init(same(options), any[EnvState])).thenReturn(mockCtx)
    when(mockLauncher.run(options, Some(mockCtx))).thenReturn(Skipped)
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine).init(same(options), any[EnvState])
    verify(mockCtx).close()
    verify(mockRepl).run()
  }
  
  "Running app with only batch option" should "not initialise env, execute options, not run repl, and not close env" in {
    
    val options = GwenOptions(batch = true)
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = mock[EvalEngine[EvalContext]]
    val app = createApp(options, mockEngine, mockRepl)
    
    when(mockLauncher.run(options, None)).thenReturn(Skipped)
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine, never()).init(same(options), any[EnvState])
    verify(mockRepl, never()).run()
  }
  
  "Running interactive app with meta file" should "execute options and run repl" in {

    val options = GwenOptions(metas = List(new File("file.meta")))
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockCtx = spy(new EvalContext(options, EnvState()))
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = mock[EvalEngine[EvalContext]]
    val app = createApp(options, mockEngine, mockRepl)

    when(mockEngine.init(same(options), any[EnvState])).thenReturn(mockCtx)
    when(mockLauncher.run(options, Some(mockCtx))).thenReturn(Passed(1))
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine).init(same(options), any[EnvState])
    verify(mockCtx).close()
    verify(mockRepl).run()
  }
  
  "Running interactive app with feature file" should "execute options and run repl" in {

    val options = GwenOptions(features = List(new File("file.feature")))
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockCtx = spy(new EvalContext(options, EnvState()))
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = mock[EvalEngine[EvalContext]]
    val app = createApp(options, mockEngine, mockRepl)

    when(mockEngine.init(same(options), any[EnvState])).thenReturn(mockCtx)
    when(mockLauncher.run(options, Some(mockCtx))).thenReturn(Passed(1))
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine).init(same(options), any[EnvState])
    verify(mockCtx).close()
    verify(mockRepl).run()
  }

  "Running batch app with meta file" should "execute options and not run repl" in {
    
    val options = GwenOptions(batch = true, metas = List(new File("file.meta")))
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = mock[EvalEngine[EvalContext]]
    val app = createApp(options, mockEngine, mockRepl)

    when(mockLauncher.run(options, None)).thenReturn(Passed(1))
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine, never()).init(same(options), any[EnvState])
    verify(mockRepl, never()).run()
    
  }
  
  "Running batch app with feature file" should "execute options and not run repl" in {
    
    val options = GwenOptions(batch = true, features = List(new File("file.feature")))
    val mockLauncher = mock[GwenLauncher[EvalContext]]
    val mockRepl = mock[GwenREPL[EvalContext]]
    val mockEngine = mock[EvalEngine[EvalContext]]
    val app = createApp(options, mockEngine, mockRepl)
    
    when(mockLauncher.run(options, None)).thenReturn(Passed(1))
    
    app.run(options, mockLauncher) should be (0)
    
    verify(mockEngine, never()).init(same(options), any[EnvState])
    verify(mockRepl, never()).run()
  }
  
}