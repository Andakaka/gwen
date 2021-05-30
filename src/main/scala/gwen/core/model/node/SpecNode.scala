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

package gwen.core.model.node

import gwen.core.model.EvalStatus
import gwen.core.model.Identifiable
import gwen.core.model.Pending
import gwen.core.model.SourceRef

/**
  * Base trait for all Gherkin specification nodes.  
  */
trait SpecNode extends Identifiable {

  /** The location in the Gherkin file or None if the node is synthetic or instantiated directly. */
  val sourceRef: Option[SourceRef]

  /** The name of the node. */
  val name: String

  /** Returns the evaluation status of this node. */
  val evalStatus: EvalStatus = Pending

  private [node] def occurrenceIn(nodes: List[SpecNode]): Int = {
    1 + indexIn(
      nodes filter { that => 
        that.name.size > 0 && that.name == this.name
      }
    )
  }

  private [node] def indexIn(nodes: List[SpecNode]): Int = {
    nodes.zipWithIndex.collectFirst {
      case (that, idx) if that.sourceRef == this.sourceRef => idx
    } getOrElse -1
  }

  override def toString: String = name

}
