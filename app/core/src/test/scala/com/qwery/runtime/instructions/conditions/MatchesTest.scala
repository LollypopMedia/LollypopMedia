package com.qwery.runtime.instructions.conditions

import com.qwery.language.models.Expression.implicits._
import com.qwery.runtime.{QweryVM, Scope}
import org.scalatest.funspec.AnyFunSpec

class MatchesTest extends AnyFunSpec {
  implicit val rootScope: Scope =
    QweryVM.executeSQL(Scope(),
      """|isExchange = x => x in ['NYSE', 'AMEX', 'NASDAQ', 'OTCBB']
         |isNumber = x => x.isNumber()
         |isString = x => x.isString()
         |""".stripMargin)._1

  describe(classOf[Matches].getSimpleName) {

    it("""should evaluate true: matches("hello", "h.*llo")""") {
      assert(Matches("hello", "h.*llo").isTrue)
    }

    it("""should evaluate false: matches("hello!", "h.*llo")""") {
      assert(Matches("hello!", "h.*llo").isFalse)
    }

    it("""should negate: matches(a, "hello")""") {
      assert(Matches("a".f, "hello").negate == Not(Matches("a".f, "hello")))
    }

    it("""should negate: not(matches(a, "hello"))""") {
      assert(Not(Matches("a".f, "hello")).negate == Matches("a".f, "hello"))
    }

    it("""should decompile: matches(name, "Lawr.*")""") {
      assert(Matches("name".f, "Lawr.*").toSQL == """name matches "Lawr.*"""")
    }

    it("should evaluate: 'Hello 123' matches 'H.* \\d+'") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|"Hello 123" matches "H.* \d+"
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate: 5678 matches isNumber") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|5678 matches isNumber
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate JSON objects (match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|response = { id: 5678, symbol: "DOG", exchange: "NYSE", lastSale: 90.67 }
           |response matches { id: isNumber, symbol: isString, exchange: isExchange, lastSale: isNumber }
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate JSON objects (no match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|response = { id: "5678", symbol: "DOG", exchange: "NYSE", "lastSale": 90.67 }
           |response matches { id: isNumber, symbol: "DOG", exchange: isString, lastSale: isNumber }
           |""".stripMargin)
      assert(result == false)
    }

    it("should evaluate an array of JSON objects (match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|response = [{ id: 5678, symbol: "DOG", exchange: "NYSE", "lastSale": 90.67 }]
           |response matches [{ id: isNumber, symbol: isString, exchange: isString, lastSale: isNumber }]
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate an array of JSON objects (no match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|response = [{ id: "5678", symbol: "DOG", exchange: "NYSE", "lastSale": 90.67 }]
           |response matches [{ id: isNumber, symbol: isString, exchange: isString, lastSale: isNumber }]
           |""".stripMargin)
      assert(result == false)
    }

    it("should evaluate a Product instance (match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|class Stock(symbol: String, exchange: String, lastSale: Double)
           |stock = new Stock(symbol: "AAPL", exchange: "NASDAQ", lastSale: 234.57)
           |stock matches Stock(symbol: "AAPL", exchange: "NASDAQ", lastSale: 234.57)
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate a Product instance (no match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|class Stock(symbol: String, exchange: String, lastSale: Double)
           |stock = new Stock(symbol: "AAPL", exchange: "NASDAQ", lastSale: 234.57)
           |stock matches Stock(symbol: "BXM", exchange: "OTCBB", lastSale: 0.7543)
           |""".stripMargin)
      assert(result == false)
    }

    it("should evaluate a Product instance with lambdas (match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|class Stock(symbol: String, exchange: String, lastSale: Double)
           |stock = new Stock(symbol: "XYZ", exchange: "NASDAQ", lastSale: 234.57)
           |stock matches Stock(symbol: isString, exchange: isExchange, lastSale: isNumber)
           |""".stripMargin)
      assert(result == true)
    }

    it("should evaluate a Product instance with lambdas (no match)") {
      val (_, _, result) = QweryVM.executeSQL(rootScope,
        """|class Stock(symbol: String, exchange: String, lastSale: Double)
           |stock = new Stock(symbol: "XYZ", exchange: null, lastSale: 234.57)
           |stock matches Stock(symbol: isString, exchange: isExchange, lastSale: isNumber)
           |""".stripMargin)
      assert(result == false)
    }

  }

}
