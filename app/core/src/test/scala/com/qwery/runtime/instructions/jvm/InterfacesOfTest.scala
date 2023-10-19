package com.qwery.runtime.instructions.jvm

import com.qwery.language.models.Expression.implicits.LifestyleExpressionsAny
import com.qwery.runtime.{QweryCompiler, QweryVM, Scope}
import org.scalatest.funspec.AnyFunSpec

class InterfacesOfTest extends AnyFunSpec {
  implicit val compiler: QweryCompiler = QweryCompiler()

  describe(classOf[InterfacesOf].getSimpleName) {

    it("should compile: interfacesOf(classOf('java.util.ArrayList'))") {
      val model = compiler.compile("interfacesOf(classOf('java.util.ArrayList'))")
      assert(model == InterfacesOf(ClassOf("java.util.ArrayList".v)))
    }

    it("should decompile: interfacesOf(classOf('java.util.ArrayList'))") {
      val model = InterfacesOf(ClassOf("java.util.ArrayList".v))
      assert(model.toSQL == """interfacesOf(classOf("java.util.ArrayList"))""")
    }

    it("should execute: interfacesOf(classOf('java.util.ArrayList'))") {
      val (_, _, result) = QweryVM.executeSQL(Scope(),
        """|interfacesOf(classOf("java.util.ArrayList"))
           |""".stripMargin)
      assert(Option(result).collect { case a: Array[_] => a.toList } contains
        Seq(
          "java.util.List", "java.util.RandomAccess", "java.lang.Cloneable", "java.io.Serializable", "java.util.Collection",
          "java.lang.Iterable").map(Class.forName))
    }

    it("should execute: interfacesOf(new `java.util.ArrayList`())") {
      val (_, _, result) = QweryVM.executeSQL(Scope(),
        """|interfacesOf(new `java.util.ArrayList`())
           |""".stripMargin)
      assert(Option(result).collect { case a: Array[_] => a.toList } contains
        Seq(
          "java.util.List", "java.util.RandomAccess", "java.lang.Cloneable", "java.io.Serializable", "java.util.Collection",
          "java.lang.Iterable").map(Class.forName))
    }

  }

}