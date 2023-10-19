package com.qwery.runtime.instructions.conditions

import com.qwery.language.HelpDoc.{CATEGORY_DATAFRAME, PARADIGM_DECLARATIVE, PARADIGM_IMPERATIVE}
import com.qwery.language.models.Expression
import com.qwery.language.{ExpressionToConditionPostParser, HelpDoc, SQLCompiler, TokenStream}
import com.qwery.runtime.instructions.conditions.Betwixt.__name
import com.qwery.runtime.instructions.conditions.RuntimeInequality.OptionComparator
import com.qwery.runtime.{QweryVM, Scope}

/**
 * SQL: `expression` betwixt `expression` and `expression`
 * @param expr the [[Expression expression]] to evaluate
 * @param from the lower bound [[Expression expression]]
 * @param to   the upper bound [[Expression expression]]
 */
case class Betwixt(expr: Expression, from: Expression, to: Expression) extends RuntimeCondition {
  override def isTrue(implicit scope: Scope): Boolean = {
    val v = Option(QweryVM.execute(scope, expr)._3)
    val a = Option(QweryVM.execute(scope, from)._3)
    val b = Option(QweryVM.execute(scope, to)._3)
    (v >= a) && (v < b)
  }

  override def toSQL: String = s"${expr.toSQL} ${__name} ${from.toSQL} and ${to.toSQL}"
}

object Betwixt extends ExpressionToConditionPostParser {
  private val __name = "betwixt"

  override def parseConditionChain(ts: TokenStream, host: Expression)(implicit compiler: SQLCompiler): Option[Betwixt] = {
    if (ts.nextIf(__name)) {
      for {
        a <- compiler.nextExpression(ts)
        _ = ts expect "and"
        b <- compiler.nextExpression(ts)
      } yield Betwixt(host, a, b)
    } else None
  }

  override def help: List[HelpDoc] = List(HelpDoc(
    name = __name,
    category = CATEGORY_DATAFRAME,
    paradigm = PARADIGM_IMPERATIVE,
    syntax = s"`value` ${__name} `to` and `from`",
    description = "determines whether the `value` is between the `to` and `from` (non-inclusive)",
    example =
      """|from (
         ||-------------------------------------------------------------------------|
         || ticker | market | lastSale | roundedLastSale | lastSaleTime             |
         ||-------------------------------------------------------------------------|
         || NKWI   | OTCBB  |  98.9501 |            98.9 | 2022-09-04T23:36:47.846Z |
         || AQKU   | NASDAQ |  68.2945 |            68.2 | 2022-09-04T23:36:47.860Z |
         || WRGB   | AMEX   |  46.8355 |            46.8 | 2022-09-04T23:36:47.862Z |
         || ESCN   | AMEX   |  42.5934 |            42.5 | 2022-09-04T23:36:47.865Z |
         || NFRK   | AMEX   |  28.2808 |            28.2 | 2022-09-04T23:36:47.864Z |
         ||-------------------------------------------------------------------------|
         |) where lastSale betwixt 28.2808 and 42.5934
         |  order by lastSale desc
         |""".stripMargin
  ))

  override def understands(ts: TokenStream)(implicit compiler: SQLCompiler): Boolean = ts is __name

}
