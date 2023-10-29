package com.qwery.runtime

import com.qwery.AppConstants
import com.qwery.database.QueryResponse
import com.qwery.database.server.QweryChartGenerator
import com.qwery.language.models.Instruction
import com.qwery.language.{HelpDoc, QweryUniverse}
import com.qwery.runtime.RuntimeFiles.RecursiveFileList
import com.qwery.runtime.devices.RowCollection
import com.qwery.runtime.instructions.expressions.GraphResult
import com.qwery.runtime.instructions.queryables.TableRendering
import com.qwery.util.ResourceHelper.AutoClose
import com.qwery.util.StringHelper.StringEnrichment
import com.qwery.util.StringRenderHelper.{StringRenderer, toProductString}
import org.scalatest.funspec.AnyFunSpec

import java.io.{File, FileWriter, PrintWriter}
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.util.Try

/**
 * Qwery README.md generator
 */
class ReadMeTest extends AnyFunSpec {
  private implicit val ctx: QweryUniverse = QweryUniverse(isServerMode = true)
  private val baseDirectory = new File(".") / "docs"
  private val imageDirectory = baseDirectory / "images"
  private val mdFile = new File("./README.md")
  private val featuredExamples: List[HelpDoc] = ctx.helpDocs.filter(_.featureTitle.nonEmpty).sortBy(_.featureTitle)

  describe(classOf[ReadMeTest].getSimpleName) {

    it("should generate a file containing examples for all documented instructions") {
      new PrintWriter(new FileWriter(mdFile)) use { out =>
        implicit val scope: Scope = ctx.createRootScope()
        out.println(
          s"""|Qwery v${AppConstants.version}
              |============
              |
              |## Table of Contents
              |* <a href="#Introduction">Introduction</a>
              |* <a href="#Project_Status">Project Status</a>
              |* <a href="#Getting_Started">Getting Started</a>
              |* <a href="#Basic_Examples">Basic Features</a>
              |""".stripMargin.trim)

        // include the basic features in the ToC
        for {
          title <- featuredExamples.flatMap(_.featureTitle)
        } out.println(s"""  * <a href="#${toAnchor(title)}">$title</a>""")

        // include the instruction examples in the ToC
        out.println("""* <a href="#Examples">Featured Examples By Category</a>""")
        val categoryMappings = ctx.helpDocs.groupBy(_.category).toList.sortBy(_._1.toLowerCase())
        for {
          (category, instructions) <- categoryMappings
        } out.println(s"""  * <a href="#${toAnchor(category)}">$category</a> (${instructions.size})""")

        // include the introduction and project status
        introduction(out)
        projectStatus(out)
        gettingStarted(out)

        // include featured examples
        out.println(
          """|<a name="Basic_Examples"></a>
             |## Basic Features
             |""".stripMargin)
        for {
          doc <- featuredExamples
          title <- doc.featureTitle
        } invoke(out, title, doc.example)

        // include examples by category
        out.println(
          """|<a name="Examples"></a>
             |""".stripMargin)
        for {
          (category, instructions) <- categoryMappings
        } processCategory(out, category, instructions)
      }
      assert(mdFile.exists())
    }

  }

  private def processCategory(out: PrintWriter, category: String, instructions: Seq[HelpDoc]): Unit = {
    out.println(
      s"""|<a name="${toAnchor(category)}"></a>
          |## $category Examples
          |<hr>
          |""".stripMargin)
    val instructionsByName = instructions.groupBy(_.name).toList.sortBy(_._1.toLowerCase)
    instructionsByName foreach { case (_, helps) =>
      helps.zipWithIndex.foreach { case (help, n) =>
        val nth = if (helps.size > 1) n match {
          case 0 => "¹"
          case 1 => "²"
          case 2 => "³"
          case _ => ""
        } else ""

        // execute and produce results
        implicit val scope: Scope = ctx.createRootScope()
        invoke(out, help, nth)
      }
    }
  }

  private def invoke(out: PrintWriter, title: String, example: String)(implicit scope: Scope): Unit = {
    // header section
    out.println(
      s"""|<a name="${toAnchor(title)}"></a>
          |### $title
          |""".stripMargin)
    out.println("```sql")
    out.println(example.trim)
    out.println("```")

    // detail section
    for {
      (scope1, _, result1) <- Try(QweryVM.executeSQL(scope, example))
      results <- resolve(result1)
    } {
      out.println("##### Results")
      if (results.contains("<img")) out.println(results)
      else {
        out.println("```sql")
        out.println(results)
        out.println("```")
      }

      showConsoleOutputs(out, scope1)
    }
  }

  private def invoke(out: PrintWriter, help: HelpDoc, nth: String)(implicit scope: Scope): Unit = {
    // header section
    out.println(
      s"""|### ${help.name}$nth (${help.category} &#8212; ${help.paradigm})
          |*Description*: ${help.description.trim}
          |""".stripMargin)
    out.println("```sql")
    out.println(help.example.trim)
    out.println("```")

    if (help.isExperimental) {
      out.println(
        s"""|<img src="docs/images/experimental.png" alt="${help.name} is marked as experimental">
            |""".stripMargin)
    }

    // detail section
    for {
      (scope1, _, result1) <- Try(QweryVM.executeSQL(scope, help.example))
      results <- resolve(result1)
    } {
      out.println("##### Results")
      if (results.contains("<img")) out.println(results)
      else {
        out.println("```sql")
        out.println(results)
        out.println("```")
      }

      showConsoleOutputs(out, scope1)
    }
  }

  private def showConsoleOutputs(out: PrintWriter, scope: Scope): Unit = {
    val system = scope.getUniverse.system

    // include STDOUT
    val consoleOut = system.stdOut.asString().trim
    if (consoleOut.nonEmpty) {
      out.println("##### Console Output")
      out.println(
        s"""|```
            |$consoleOut
            |```""".stripMargin)
    }

    // include STDERR
    val consoleErr = system.stdErr.asString().trim
    if (consoleErr.nonEmpty) {
      out.println("##### Console Error")
      out.println(
        s"""|```
            |$consoleErr
            |```""".stripMargin)
    }
  }

  private def introduction(out: PrintWriter): Unit = {
    out.println(
      """|<a name="Introduction"></a>
         |## Introduction
         |Qwery is a general-purpose programming/scripting language for the JVM.
         |Features include:
         |* Native support for Scala and Java classes, objects and packages.
         |* Native support for JSON (arrays, dictionaries and objects).
         |* Native support for Maven package repositories.
         |* Data-oriented types - Dataframes, BLOB/CLOB and Matrices and Vectors.
         |* Multi-paradigm programming model - declarative/SQL, functional and object-oriented.
         |""".stripMargin)
  }

  private def gettingStarted(out: PrintWriter): Unit = {
    val coreAssembly = s"core-assembly-$version.jar"
    val jdbcAssembly = s"jdbc-driver-assembly-$version.jar"
    out.println(
      s"""|<a name="Getting_Started"></a>
          |## Getting Started
          |### To build Qwery Core (Client and Server)
          |```bash
          |sbt "project core" clean assembly
          |```
          |The Jar binary should be `./app/core/target/scala-2.13/$coreAssembly`
          |
          |### To build the Qwery JDBC driver
          |```bash
          |sbt "project jdbc_driver" clean assembly
          |```
          |The Jar binary should be `./app/jdbc-driver/target/scala-2.13/$jdbcAssembly`
          |
          |### Run Query CLI
          |```bash
          |sbt "project core" run
          |```
          |OR
          |```bash
          |java -jar ./app/core/target/scala-2.13/$coreAssembly
          |```
          |""".stripMargin)
  }

  private def projectStatus(out: PrintWriter): Unit = {
    out.println(
      """|<a name="Project_Status"></a>
         |## Project Status
         |
         |Unstable/Preview &#8212; it works... but the language parser is a little temperamental.
         |""".stripMargin)
  }

  @tailrec
  private def resolve(outcome: Any)(implicit scope: Scope): Option[String] = {
    outcome match {
      case null | None => None
      case Some(rc: RowCollection) => resolve(rc)
      case dr: GraphResult =>
        val imageFile = QweryChartGenerator.generateFile(imageDirectory, dr)
        Some(
          s"""|<div style="width: 100%">
              |<img src="${imageFile.getPath}">
              |</div>
              |""".stripMargin)
      case fu: Future[_] => resolve(Try(Await.result(fu, 10.seconds)).toOption)
      case rc: RowCollection => if (rc.nonEmpty) Some(rc.tabulate().mkString("\n")) else None
      case qr: QueryResponse => Some(qr.tabulate().mkString("\n"))
      case sc: Scope => resolve(sc.toRowCollection)
      case tr: TableRendering => resolve(tr.toTable)
      case in: Instruction => Some(in.toSQL)
      case pr: Product => Some(toProductString(pr))
      case s: String if s.trim.isEmpty => None
      case s: String if s.isQuoted => Some(s)
      case xx => Some(xx.render)
    }
  }

  private def toAnchor(name: String): String = {
    name.map {
      case c if c.isLetterOrDigit => c
      case _ => '_'
    }.replace("__", "_")
  }

}