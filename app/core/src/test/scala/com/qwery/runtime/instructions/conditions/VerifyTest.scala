package com.qwery.runtime.instructions.conditions

import com.qwery.language.models.Expression.implicits._
import com.qwery.runtime.instructions.VerificationTools
import com.qwery.runtime.{QweryCompiler, QweryVM, Scope}
import org.scalatest.funspec.AnyFunSpec

class VerifyTest extends AnyFunSpec with VerificationTools {
  implicit val compiler: QweryCompiler = QweryCompiler()

  describe(classOf[Verify].getSimpleName) {

    it("should interpret 'verify status is 200'") {
      val scope0 = Scope()
        .withVariable(name = "status", code = 200.v, isReadOnly = false)
      val verify = Verify(EQ("status".f, 200.v))
      val (_, _, result) = QweryVM.execute(scope0, verify)
      assert(result == true)
    }

    it("should interpret 'verify status isnt 200'") {
      val scope0 = Scope()
        .withVariable(name = "status", code = 200.v, isReadOnly = false)
      val verify = Verify(NEQ("status".f, 200.v))
      val (_, _, result) = QweryVM.execute(scope0, verify)
      assert(result == false)
    }

    it("should compile: verify status is 200 ^^^ 'Notebook created'") {
      val model = compiler.compile(
        """|verify statusCode is 200
           |  ^^^ "Notebook created"
           |""".stripMargin)
      assert(model == Verify(condition = Is("statusCode".f, 200.v), message = Some("Notebook created".v)))
    }

    it("should compile a complete scenario") {
      compiler.compile(
        """|scenario 'Create a new notebook' {
           |  val responseA = http post 'http://{{host}}:{{port}}/api/notebooks/notebooks' <~ { name: "ShockTrade" }
           |  val notebook_id = responseA.body.id
           |  verify responseA.statusCode is 200
           |    ^^^ "Notebook created"
           |}
           |""".stripMargin)
    }

  }

}
