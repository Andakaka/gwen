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

package gwen.dsl

import gwen._

import scala.jdk.CollectionConverters._

import io.cucumber.messages.{Messages => Cucumber }

import java.io.File

trait Identifiable {
  val uuid: String = UUIDGenerator.nextId
  def nodeType: NodeType.Value
}

object Root extends Identifiable {
  override val uuid: String = UUIDGenerator.baseId
  override def nodeType: NodeType.Value = NodeType.Root
}

/**
  * Base trait for capturing a feature spec in an abstract syntax tree.  
  * An spec node is the raw output produced by the [[GherkinParser]].
  */
trait SpecNode extends Identifiable {

  /** The location in the Gherkin file or None if the node is synthetic or instantiated directly. */
  val sourceRef: Option[SourceRef]

  /** The name of the node. */
  val name: String

  /** Returns the evaluation status of this node. */
  val evalStatus: EvalStatus = Pending
  
  /**
    * Gets the identical occurence number of the current node in given nodes.
    * 
    * @param nodes the nodes to check in
    * @return 1 if the node only appears once, or the number of times it appears
    */
  private [dsl] def occurrenceIn(nodes: List[SpecNode]): Int = {
    (nodes filter { that => 
      that.name.size > 0 && that.name == this.name
    } zipWithIndex).collectFirst {
      case (that, idx) 
        if that.sourceRef.map(_.toString).getOrElse("") == 
          this.sourceRef.map(_.toString).getOrElse("") => idx + 1 
    } getOrElse 0
  }

  override def toString: String = name

}

/**
 * Abstract syntax tree of a successfully parsed feature.
 * The [[GherkinParser]] parses all plain text features into a tree of
 * this type.  The [[gwen.eval.GwenInterpreter interpreter]] normalises 
 * the tree before passing it down to the 
 * [[gwen.eval.EvalEngine evaluation engine]] and lower layers for 
 * processing.
 *
 * @param feature the feature
 * @param background optional background
 * @param scenarios list of scenarios
 * @param featureFile optional source feature file
 * @param metaSpecs optional list of meta specs
 */

case class FeatureSpec(
    feature: Feature, 
    background: Option[Background], 
    scenarios: List[Scenario],
    rules: List[Rule],
    featureFile: Option[File],
    metaSpecs: List[FeatureSpec]) extends SpecNode {

  override val name: String = feature.name
  override val sourceRef: Option[SourceRef] = feature.sourceRef
  override def nodeType: NodeType.Value = NodeType.withName(specType.toString)

  def specType: SpecType.Value = feature.specType
  def isMeta: Boolean = SpecType.isMeta(specType)

  /** Resource id */
  def uri = featureFile.map(_.uri).getOrElse(uuid)

  /**
    * Gets the list of all steps contained in the feature spec. The list includes
    * all meta steps (if any) and all scenario steps (including any background 
    * steps).
    * 
    * @return a list containing all the steps (or an empty list if none exist)
    */
  def steps: List[Step] = rules.flatMap(_.allSteps) ++ scenarios.flatMap(_.allSteps)

  def evalScenarios: List[Scenario] = scenarios.flatMap(_.evalScenarios) ++ rules.flatMap(_.evalScenarios)

  /** Gets all attachments. */
  def attachments: List[(String, File)] = steps.flatMap(_.deepAttachments)

  /** Gets the number of sustained errors. */
  def sustainedCount: Int = {
    steps.flatMap { s1 =>
      s1.stepDef.map { s2 =>
        s2.allSteps.flatMap { s3 =>
          s3.stepDef map { s4 => 
            s4.allSteps 
          } getOrElse List(s3)
        }
      } getOrElse List(s1)
    } count(_.evalStatus.status == StatusKeyword.Sustained)
  }
  
  /** Returns the evaluation status of this feature spec. */
  override val evalStatus: EvalStatus = {
    val ss = steps.map(_.evalStatus)
    val specStatus = EvalStatus(ss)
    metaSpecs match {
      case Nil => specStatus
      case _ =>
        val totalStatus = EvalStatus((metaSpecs.flatMap(_.steps) ++ steps).map(_.evalStatus))
        specStatus match {
          case Passed(_) => Passed(totalStatus.nanos)
          case _ => totalStatus
        }
    }
  }

  def copy(
      withFeature: Feature = feature,
      withBackground: Option[Background] = background,
      withScenarios: List[Scenario] = scenarios,
      withRules: List[Rule] = rules,
      withFeatureFile: Option[File] = featureFile,
      withMetaSpecs: List[FeatureSpec] = metaSpecs): FeatureSpec = {
    FeatureSpec(withFeature, withBackground, withScenarios, withRules, withFeatureFile, withMetaSpecs)
  }

  def withNodePath(path: String): FeatureSpec = {
    val featurePath = SourceRef.nodePath(s"$path/${feature.name}", 1)
    copy(
      withFeature = feature.withNodePath(featurePath),
      withBackground = background.map(bg => bg.withNodePath(SourceRef.nodePath(s"$featurePath/${bg.name}", 1))),
      withScenarios = scenarios map { s =>
        s.withNodePath(SourceRef.nodePath(s"$featurePath/${s.name}", s.occurrenceIn(scenarios)))
      },
      withRules = rules map { r =>
        r.withNodePath(SourceRef.nodePath(s"$featurePath/${r.name}", r.occurrenceIn(rules)))
      }
    )
  }
  
}

object FeatureSpec {
  def apply(uri: String, spec: Cucumber.GherkinDocument, specFile: Option[File]): FeatureSpec = {
    val feature = Feature(uri, spec.getFeature)
    val background = spec.getFeature.getChildrenList.asScala.toList.filter(_.hasBackground).headOption.map(x => Background(uri, x.getBackground))
    val scenarios = spec.getFeature.getChildrenList.asScala.toList.filter(_.hasScenario).map(x => Scenario(uri, x.getScenario))
    val rules = spec.getFeature.getChildrenList.asScala.toList.filter(_.hasRule()).map(x => Rule(uri, x.getRule()))
    FeatureSpec(feature, background, scenarios, rules, specFile, Nil)
  }
}

/**
  * Captures a gherkin feature node.
  *
  * @param language the language identifier (example: en for English)
  * @param sourceRef the location in source
  * @param tags list of tags
  * @param keyword the Gherkin keyword for this Feature
  * @param name the feature name
  * @param description optional description
  */
case class Feature(
    language: String, 
    sourceRef: Option[SourceRef],
    tags: List[Tag],
    keyword: String, 
    name: String, 
    description: List[String]) extends SpecNode {

  def specType = if (sourceRef.map(_.uri.contains(".meta")).getOrElse(false)) SpecType.Meta else SpecType.Feature

  override def nodeType: NodeType.Value = NodeType.Feature

  def copy(
      withLanguage: String = language, 
      withSourceRef: Option[SourceRef] = sourceRef,
      withTags: List[Tag] = tags,
      withKeyword: String = keyword, 
      withName: String = name, 
      withDescription: List[String] = description): Feature = {
    Feature(withLanguage, withSourceRef, withTags, withKeyword, withName, withDescription)
  }

  def withNodePath(path: String): Feature = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path))
    )
  }

}

object Feature {
  def apply(uri: String, feature: Cucumber.GherkinDocument.Feature): Feature = {
    Feature(
      feature.getLanguage,  
      Option(feature.getLocation).map(loc => SourceRef(uri, loc)),
      Option(feature.getTagsList).map(_.asScala.toList).getOrElse(Nil).map(t => Tag(uri, t)), 
      feature.getKeyword,
      feature.getName, 
      Option(feature.getDescription).filter(_.length > 0).map(_.split("\n").toList.map(_.trim)).getOrElse(Nil).distinct
    )
  }
}

/**
  * Captures a gherkin background node.
  *
  * @param sourceRef the location in source
  * @param keyword the Gherkin keyword for this Background
  * @param name the background name
  * @param description optional background description
  * @param steps list of background steps
 */
case class Background(
    sourceRef: Option[SourceRef],
    keyword: String, 
    name: String, 
    description: List[String], 
    steps: List[Step]) extends SpecNode {

  override def nodeType: NodeType.Value = NodeType.Background

  def gwtOrder: List[String] = steps.map(_.keyword).filter(k => !StepKeyword.isAnd(k))

  override val evalStatus: EvalStatus = EvalStatus(steps.map(_.evalStatus))

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withKeyword: String = keyword, 
      withName: String = name, 
      withDescription: List[String] = description, 
      withSteps: List[Step] = steps): Background = {
    Background(withSourceRef, withKeyword, withName, withDescription, withSteps)
  }

  def withNodePath(path: String): Background = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path)),
      withSteps = steps map { s =>
        s.withNodePath(SourceRef.nodePath(s"$path/${s.expression}", s.occurrenceIn(steps)))
      }
    )
  }
  
}

object Background {
  def apply(uri: String, background: Cucumber.GherkinDocument.Feature.Background): Background = {
    Background(
      Option(background.getLocation).map(loc => SourceRef(uri, loc)),
      background.getKeyword,
      background.getName,
      Option(background.getDescription).filter(_.length > 0).map(_.split("\n").toList.map(_.trim)).getOrElse(Nil),
      Option(background.getStepsList).map(_.asScala.toList).getOrElse(Nil).map(s => Step(uri, s))
    )
  }
}

/**
  * Captures a gherkin rule.
  * 
  * @param sourceRef the location in source
  * @param keyword the Gherkin keyword for this Rule
  * @param name the rule name
  * @param description optional description
  * @param background optional background
  * @param scenarios list of scenarios (or examples)
  */
case class Rule(
    sourceRef: Option[SourceRef],
    keyword: String,
    name: String,
    description: List[String],
    background: Option[Background],
    scenarios: List[Scenario]) extends SpecNode {
  
  override def nodeType: NodeType.Value = NodeType.Rule

  /**
    * Gets the list of all steps contained in the rule. The list includes
    * all meta steps (if any) and all scenario steps (including any background 
    * steps).
    * 
    * @return a list containing all the steps (or an empty list if none exist)
    */
  def allSteps: List[Step] = background.map(_.steps).getOrElse(Nil) ++ scenarios.flatMap(_.allSteps)

  def evalScenarios: List[Scenario] = scenarios.flatMap(_.evalScenarios)
  
  /** Returns the evaluation status of this rule. */
  override val evalStatus: EvalStatus = EvalStatus(allSteps.map(_.evalStatus))

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withKeyword: String = keyword,
      withName: String = name,
      withDescription: List[String] = description,
      withBackground: Option[Background] = background,
      withScenarios: List[Scenario] = scenarios): Rule = {
    Rule(withSourceRef, withKeyword, withName, withDescription, withBackground, withScenarios)
  }

  def occurrenceIn(parent: Identifiable): Int = {
    parent match {
      case spec: FeatureSpec =>
        occurrenceIn(spec.rules)
      case _ => 0
    }
  }

  def withNodePath(path: String): Rule = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path)),
      withBackground = background.map(bg => bg.withNodePath(SourceRef.nodePath(s"$path/${bg.name}", 1))),
      withScenarios = scenarios map { s => 
        s.withNodePath(SourceRef.nodePath(s"$path/${s.name}", s.occurrenceIn(this)))
      }
    )
  }

}

object Rule {
  def apply(uri: String, rule: Cucumber.GherkinDocument.Feature.FeatureChild.Rule): Rule = {
    Rule(
      Option(rule.getLocation).map(loc => SourceRef(uri, loc)),
      rule.getKeyword,
      rule.getName,
      Option(rule.getDescription).filter(_.length > 0).map(_.split("\n").toList.map(_.trim)).getOrElse(Nil),
      rule.getChildrenList.asScala.toList.filter(_.hasBackground).headOption.map(x => Background(uri, x.getBackground)),
      rule.getChildrenList.asScala.toList.filter(_.hasScenario).map(x => Scenario(uri, x.getScenario))
    )
  }
}

/**
  * Captures a gherkin scenario.
  * 
  * @param sourceRef the location in source
  * @param tags list of tags
  * @param keyword the Gherkin keyword
  * @param name the scenario name
  * @param description optional description
  * @param background optional background
  * @param steps list of scenario steps
  * @param examples optional list of examples (scenario outline entries)
  * @param params parameters (stepdef params or outline example row data)
  */
case class Scenario(
    sourceRef: Option[SourceRef],
    tags: List[Tag],
    keyword: String,
    name: String,
    description: List[String],
    background: Option[Background],
    steps: List[Step],
    examples: List[Examples],
    params: List[(String, String)],
    callerParams: List[(String, String)]) extends SpecNode {

  override def nodeType: NodeType.Value = {
    if (isStepDef) {
      NodeType.StepDef
    } else {
      NodeType.Scenario
    }
  }
  
  /**
    * Returns a list containing all steps.
    */
  def allSteps: List[Step] = background.map(_.steps).getOrElse(Nil) ++ (if (!isOutline) steps else examples.flatMap(_.allSteps))
  
  def evalScenarios: List[Scenario] = 
    if (isStepDef) Nil
    else if(isOutline) examples.flatMap(_.scenarios)
    else List(this)
  
  def isOutline: Boolean = examples.nonEmpty || tags.exists(_.name == ReservedTags.Examples.toString)
  def isExpanded: Boolean = examples.flatMap(_.scenarios).nonEmpty 
  def isStepDef: Boolean = tags.exists(_.name == ReservedTags.StepDef.toString)
  def isForEach: Boolean = tags.exists(_.name == ReservedTags.ForEach.toString)
  def isDataTable: Boolean = tags.exists(_.name.startsWith(ReservedTags.DataTable.toString))
  def isSynchronized: Boolean = tags.map(_.name).exists { 
    name => name == ReservedTags.Synchronized.toString || name == ReservedTags.Synchronised.toString
  }
  def isSynthetic: Boolean = Tag.findByName(tags, ReservedTags.Synthetic.toString).nonEmpty
  def isVirtual: Boolean = name.contains(s"$ZeroChar")
  
  def attachments: List[(String, File)] = {
    allSteps.flatMap(step => step.deepAttachments)
  }
  
  /** Returns the evaluation status of this scenario. */
  override val evalStatus: EvalStatus =
    if (isOutline && examples.flatMap(_.scenarios).isEmpty) Pending
     else EvalStatus(allSteps.map(_.evalStatus), ignoreSustained = !isStepDef)

  def behaviorTag: Option[Tag] = tags.find(tag => BehaviorType.values.exists(_.toString == tag.name))

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withTags: List[Tag] = tags,
      withKeyword: String = keyword,
      withName: String = name,
      withDescription: List[String] = description,
      withBackground: Option[Background] = background,
      withSteps: List[Step] = steps,
      withExamples: List[Examples] = examples,
      withParams: List[(String, String)] = params,
      withCallerParams: List[(String, String)] = callerParams): Scenario = {
    Scenario(withSourceRef, withTags, withKeyword, withName, withDescription, withBackground, withSteps, withExamples, withParams, withCallerParams)
  }

  def occurrenceIn(parent: Identifiable): Int = {
    parent match {
      case spec: FeatureSpec =>
        occurrenceIn(spec.scenarios)
      case rule: Rule =>
        occurrenceIn(rule.scenarios)
      case examples: Examples =>
        occurrenceIn(examples.scenarios)
      case _ => 0
    }
  }

  def withNodePath(path: String): Scenario = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path)),
      withBackground = background.map(bg => bg.withNodePath(SourceRef.nodePath(s"$path/${bg.name}", 1))),
      withSteps = steps map { s => 
        s.withNodePath(SourceRef.nodePath(s"$path/${s.expression}", s.occurrenceIn(this)))
      },
      withExamples = examples map { e => 
        e.withNodePath(SourceRef.nodePath(s"$path/${e.name}", 1))
      }
    )
  }

  def withCallerParams(caller: Identifiable): Scenario = {
    val names = callerParams map { case (n, _) => n }
    caller match {
      case step: Step => 
        step.cumulativeParams filter { case (name, _) => 
          !names.contains(name)
        } match {
          case Nil => this
          case sParams => copy(withCallerParams = callerParams ++ sParams)
        }
      case _ => this
    }
  }

  def cumulativeParams: List[(String, String)] = {
    val names = params map { case (n, _) => n }
    params ++ (
      callerParams filter { case (name, _) => 
        !names.contains(name)
      }
    )
  }
  
}

object Scenario {
  def apply(uri: String, scenario: Cucumber.GherkinDocument.Feature.Scenario): Scenario = {
    def tags = Option(scenario.getTagsList).map(_.asScala.toList).getOrElse(Nil).map(t => Tag(uri, t)).distinct
    Scenario(
      Option(scenario.getLocation).map(loc => SourceRef(uri, loc)),
      tags,
      keywordFor(tags, scenario.getKeyword),
      scenario.getName,
      Option(scenario.getDescription).filter(_.length > 0).map(_.split("\n").toList.map(_.trim)).getOrElse(Nil),
      None,
      Option(scenario.getStepsList).map(_.asScala.toList).getOrElse(Nil).map(s => Step(uri, s)),
      scenario.getExamplesList.asScala.toList.zipWithIndex map { case (examples, index) => Examples(uri, examples, index) },
      Nil,
      Nil
    )
  }
  def keywordFor(scenario: Scenario): String = keywordFor(scenario.tags, scenario.keyword)
  def keywordFor(tags: List[Tag], keyword: String): String = {
    tags.map(_.name) find { name =>
      name == ReservedTags.StepDef.toString || name == ReservedTags.ForEach.toString || name == ReservedTags.If.toString || name == ReservedTags.Until.toString || name == ReservedTags.While.toString
    } getOrElse {
      keyword.trim
    }
  }
}

/**
  * Captures a gherkin scenario outline example group.
  *
  * @param sourceRef the location in source
  * @param tags list of tags
  * @param keyword the Gherkin keyword for this Examples clause
  * @param name the example name
  * @param description option description lines
  * @param table header and body data (tuple of line position and rows of data)
  * @param scenarios list of expanded scenarios
  */
case class Examples(
    sourceRef: Option[SourceRef],
    tags: List[Tag], 
    keyword: String, 
    name: String, 
    description: List[String], 
    table: List[(Int, List[String])], 
    scenarios: List[Scenario]) extends SpecNode {

  def nodeType: NodeType.Value = NodeType.Examples

  /**
    * Returns a list containing all the background steps (if any) followed by
    * all the scenario steps.
    */
  def allSteps: List[Step] = scenarios.flatMap(_.allSteps)

  /** Returns the evaluation status of this examples group. */
  override val evalStatus: EvalStatus = EvalStatus(scenarios.map(_.evalStatus))

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withTags: List[Tag] = tags, 
      withKeyword: String = keyword, 
      withName: String = name, 
      withDescription: List[String] = description, 
      withTable: List[(Int, List[String])] = table, 
      withScenarios: List[Scenario] = scenarios): Examples = {
    Examples(withSourceRef, withTags, withKeyword, withName, withDescription, withTable, withScenarios)
  }

  def occurrenceIn(parent: Identifiable): Int = {
    parent match {
      case scenario: Scenario =>
        occurrenceIn(scenario.examples)
      case _ => 0
    }
  }

  def withNodePath(path: String): Examples = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path)),
      withScenarios = scenarios map { s => 
        s.withNodePath(SourceRef.nodePath(s"$path/${s.name}", s.occurrenceIn(this)))
      }
    )
  }

}

object Examples {
  def apply(uri: String, examples: Cucumber.GherkinDocument.Feature.Scenario.Examples, index: Int): Examples = {
    val header = examples.getTableHeader
    if (header == null) {
      Errors.syntaxError(
        s"Failed to read table body. Possible syntax error or missing column delimiter in table",
        uri,
        examples.getLocation.getLine,
        examples.getLocation.getColumn)
    }
    val body = examples.getTableBodyList
    if (body == null) {
      Errors.syntaxError(
        s"Failed to read table header. Possible syntax error or missing column delimiter in table",
        uri,
        examples.getLocation.getLine,
        examples.getLocation.getColumn)
    }
    Examples(
      Option(examples.getLocation).map(loc => SourceRef(uri, loc)),
      Option(examples.getTagsList).map(_.asScala.toList).getOrElse(Nil).map(t =>Tag(uri, t)),
      examples.getKeyword,
      examples.getName,
      Option(examples.getDescription).filter(_.length > 0).map(_.split("\n").toList.map(_.trim)).getOrElse(Nil),
      (header.getLocation.getLine, header.getCellsList.asScala.toList.map(_.getValue)) ::
        body.iterator.asScala.toList.map { row =>
          (row.getLocation.getLine, row.getCellsList.asScala.toList.map(_.getValue))
        },
      Nil
    )
  }
}

/**
  * Captures a gherkin tag.
  *
  * @param sourceRef the location in source
  * @param name name the tag name
  */
case class Tag(sourceRef: Option[SourceRef], name: String, value: Option[String]) extends SpecNode {
  
  def nodeType: NodeType.Value = NodeType.Tag
  def isAnnotation = value.nonEmpty || ReservedTags.values.filter(_ != ReservedTags.Ignore).exists(_.toString == name)
  def isMarker = value.isEmpty && !isAnnotation

  if (name.matches("""\s""")) {
    Errors.invalidTagError(s"Whitespace not allowed in tag name '$name'")
  }
  value foreach { v=>
    if (v.matches("""\s""")) {
      Errors.invalidTagError(s"Whitespace not allowed in @$name tag value '$v'")
    }
  }
    
  /** Returns a string representation of this tag. */
  override def toString: String = s"@$name${value.map(v => s"""("$v")""").getOrElse("")}"

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withName: String = name,
      withValue: Option[String] = value): Tag = {
    Tag(withSourceRef, withName, withValue)
  }
  
}

object Tag {

  def apply(uri: String, tag: Cucumber.GherkinDocument.Feature.Tag): Tag = {
    val pos = Option(tag.getLocation).map(loc => SourceRef(uri, loc))
    Tag(pos, tag.getName) tap { t =>
      if (t.name == ReservedTags.DataTable.toString) {
        DataTable.checkTagSyntax(t)
      }
    }
  }

  def apply(name: ReservedTags.Value): Tag = {
    Tag(None, name.toString, None)
  }
  def apply(name: ReservedTags.Value, value: String): Tag = {
    Tag(None, name.toString, Option(value))
  }
  def apply(tagString: String): Tag = {
    apply(None, tagString)
  }
  def apply(sourceRef: Option[SourceRef], tagString: String): Tag = {
    if (tagString.matches("""\s""")) {
      Errors.invalidTagError(s"Whitespace not allowed in tag '$tagString'")
    }
    tagString match {
      case r"""~?@(.*?)$n\("(.*?)"$v\)""" => Tag(sourceRef, n, Option(v))
      case r"""~?@(.*?)$n""" => Tag(sourceRef, n, None)
      case _ => Errors.invalidTagError(s"Invalid tag syntax: $tagString")
    }
  }
  def findTagValue(tags: List[Tag], name: String): Option[String] = {
    findByName(tags, name).find(_.name == name).flatMap(_.value)
  }
  def findByName(tags: List[Tag], name: String): Option[Tag] = {
    findAllByName(tags, name).headOption
  }
  def findAllByName(tags: List[Tag], name: String): List[Tag] = {
    tags.filter(_.name == name)
  }
  
}

/**
  * Captures a gherkin step.
  *
  * @param sourceRef the location in source
  * @param keyword keyword identifier (Given, When, Then, etc..)
  * @param name the step name (that is: the text following the step keyword)
  * @param status optional evaluation status (default = Pending)
  * @param attachments file attachments as name-file pairs (default = Nil)
  * @param stepDef optional evaluated step def
  * @param table data table (List of tuples of line position and rows of data)
  * @param docString optional tuple of line, content, and content type
  * @param evalStatus the evaluation status of the step
  * @param params optional step parameters
  */
case class Step(
    sourceRef: Option[SourceRef],
    keyword: String,
    name: String,
    attachments: List[(String, File)],
    stepDef: Option[Scenario],
    table: List[(Int, List[String])],
    docString: Option[(Int, String, Option[String])],
    override val evalStatus: EvalStatus,
    params: List[(String, String)],
    callerParams: List[(String, String)]) extends SpecNode {

  def nodeType: NodeType.Value = NodeType.Step
  val isVirtual: Boolean = name.contains(s"$ZeroChar")
  
  def expression: String = docString map { case (_, content, _) =>
    val lines = content.split("""\r?\n""")
    s"""$name "${lines(0)}${if (lines.length > 1) "..." else ""}""""
  } getOrElse(name)

  /** Returns the given value if the step has no docString or the docString content otherwise. */
  def orDocString(value: String): String = docString.map(_._2).getOrElse(value)

  def docStringify: Option[Step] = {
    name match {
      case r"""^(?s)(.*)$prefix"\$$<(.+?)$param>"$$""" if docString.isEmpty =>
        Some(
          this.copy(
            withName = prefix.trim,
            withDocString = Some((0, s"<$param>", None))
          )
        )
      case _ => None
    }
  }

  def deepSteps: List[Step] = {
    List(this) ++ (stepDef map { sd => 
      sd.steps.flatMap(_.deepSteps)
    } getOrElse Nil)
  }

  def deepAttachments: List[(String, File)] = {
    deepSteps.flatMap(_.attachments)
  }

  /** Returns a string representation of this step. */
  override def toString: String = s"$keyword ${expression}"

  def copy(
      withSourceRef: Option[SourceRef] = sourceRef,
      withKeyword: String = keyword,
      withName: String = name,
      withAttachments: List[(String, File)] = attachments,
      withStepDef: Option[Scenario] = stepDef,
      withTable: List[(Int, List[String])] = table,
      withDocString: Option[(Int, String, Option[String])] = docString,
      withEvalStatus: EvalStatus = evalStatus,
      withParams: List[(String, String)] = params,
      withCallerParams: List[(String, String)] = callerParams): Step = {
    Step(withSourceRef, withKeyword, withName, withAttachments, withStepDef, withTable, withDocString, withEvalStatus, withParams, withCallerParams)
  }

  def occurrenceIn(parent: Identifiable): Int = {
    parent match {
      case background: Background =>
        occurrenceIn(background.steps)
      case scenario: Scenario =>
        occurrenceIn(scenario.steps)
      case _ => 0
    }
  }

  def withNodePath(path: String): Step = {
    copy(
      withSourceRef = sourceRef.map(_.withNodePath(path)),
      withStepDef = stepDef.map { sd =>
        sd.withNodePath(SourceRef.nodePath(s"$path/${sd.name}", 1))
      }
    )
  }

  def withCallerParams(caller: Identifiable): Step = {
    val names = callerParams map { case (n, _) => n }
    caller match {
      case scenario: Scenario => 
        scenario.cumulativeParams filter { case (name, _) => 
          !names.contains(name)
        } match {
          case Nil => this
          case sParams => copy(withCallerParams = callerParams ++ sParams)
        }
      case _ => this
    }
  }

  def cumulativeParams: List[(String, String)] = {
    val names = params map { case (n, _) => n }
    params ++ (
      callerParams filter { case (name, _) => 
        !names.contains(name)
      }
    )
  }

  lazy val errorTrails: List[List[Step]] = {
    if (EvalStatus.isError(evalStatus.status)) {
      stepDef map { sd => 
        sd.allSteps.filter(step => EvalStatus.isError(step.evalStatus.status)).flatMap { step => 
          step.errorTrails map { trace => 
            this :: trace
          }
        }
      } getOrElse List(List(this))
    } else Nil
  }
}

object Step {
  def apply(uri: String, step: Cucumber.GherkinDocument.Feature.Step): Step = {
    val dataTable = Option(step.getDataTable).map { dt =>
      dt.getRowsList.asScala.toList map { row =>
        (row.getLocation.getLine, row.getCellsList.asScala.toList.map(_.getValue))
      }
    } getOrElse Nil
    val docString = Option(step.getDocString()).filter(_.getContent().trim.length > 0) map { ds =>
      (ds.getLocation.getLine, ds.getContent, Option(ds.getMediaType).filter(_.trim.length > 0))
    }
    Step(
      Option(step.getLocation).map(loc => SourceRef(uri, loc)),
      step.getKeyword.trim, 
      step.getText, 
      Nil, 
      None, 
      dataTable, 
      docString, 
      Pending,
      Nil,
      Nil)
  }
  def errorTrails(node: SpecNode): List[List[Step]] = node match {
    case b: Background => b.steps.flatMap(_.errorTrails)
    case s: Scenario => s.allSteps.flatMap(_.errorTrails)
    case e: Examples => e.allSteps.flatMap(_.errorTrails)
    case r: Rule => r.allSteps.flatMap(_.errorTrails)
    case s: Step => s.errorTrails
    case _ => Nil
  }
}
