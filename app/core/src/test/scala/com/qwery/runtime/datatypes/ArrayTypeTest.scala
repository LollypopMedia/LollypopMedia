package com.qwery.runtime.datatypes

import com.qwery.language.models.ColumnType
import com.qwery.language.models.Expression.implicits.LifestyleExpressions
import com.qwery.language.{ColumnTypeParser, TokenStream}
import com.qwery.runtime.datatypes.DataTypeFunSpec.FakeNews
import com.qwery.runtime.{QweryCompiler, Scope}
import com.qwery.util.DateHelper

import java.util.{Date, UUID}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * ArrayType Tests
 */
class ArrayTypeTest extends DataTypeFunSpec {
  private implicit val compiler: QweryCompiler = QweryCompiler()

  describe(classOf[ArrayType].getSimpleName) {

    it("should compile to ColumnType: Int[]") {
      val columnType = ColumnTypeParser.nextColumnType(TokenStream("Int[]"))
      assert(columnType == ColumnType.array("Int".ct))
    }

    it("should compile to DataType: Int[]") {
      val dataType = DataType(ColumnType.array("Int".ct))(Scope())
      assert(dataType == ArrayType(Int32Type))
    }

    it("should decompile: Int[]") {
      val model = ArrayType(Int32Type)
      assert(model.toSQL == "Int[]")
    }

    it("should detect ArrayType column types containing values") {
      val expected = Inferences.fromValue(value = Array(1, 2, 3))
      assert(expected == ArrayType(Int32Type, capacity = Some(3)))
    }

    it("should detect ArrayType column types from classes") {
      val expected = Inferences.fromClass(Array(1, 2, 3).getClass)
      assert(expected == ArrayType(Int32Type))
    }

    it("should encode/decode ArrayType values") {
      verifyArray(BooleanType, expected = Array[Boolean](true, false, true))
      verifyArray(CharType, expected = "WHY DID THE CHICKEN".toCharArray)
      verifyArray(DateTimeType, expected = Array[Date](DateHelper("2021-08-05T19:23:12.000Z"), DateHelper("2021-08-05T19:23:12.000Z")))
      verifyArray(Float64Type, expected = Array[Double](123.456, 543.086, 765.1111, 0.456))
      verifyArray(EnumType(Seq("Apple", "Banana", "Cherry", "Date")), expected = Array("Apple", "Banana", "Cherry", "Date"))
      verifyArray(Float32Type, expected = Array[Float](123.456f, 543.086f, 765.1111f, 0.456f))
      verifyArray(Int8Type, expected = "WHY DID THE CHICKEN".getBytes)
      verifyArray(Int16Type, expected = Array[Short](123, 543, 765, 456))
      verifyArray(Int32Type, expected = Array[Int](123, 543, 765, 456))
      verifyArray(Int64Type, expected = Array[Long](1234567L, 543086L, 7651111L, 456000L))
      verifyArray(IntervalType, expected = Array[FiniteDuration](100.millis, 2.seconds, 3.minutes, 4.hours, 5.days))
      verifyArray(AnyType, expected = Array(FakeNews("Apple"), DateHelper.now, Seq(1, 2, 3), UUID.randomUUID()))
      verifyArray(StringType, expected = Array[String]("Apple", "Banana", "Cherry", "Date"))
      verifyArray(UUIDType, expected = Array[UUID](UUID.randomUUID(), UUID.randomUUID()))
    }

  }

}
