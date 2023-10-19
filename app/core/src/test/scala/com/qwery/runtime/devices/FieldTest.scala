package com.qwery.runtime.devices

import com.qwery.language.models.Expression.implicits.LifestyleExpressionsAny
import com.qwery.runtime.Scope
import com.qwery.runtime.datatypes.StringType
import com.qwery.runtime.devices.Field.ColumnToFieldExtension
import com.qwery.runtime.instructions.VerificationTools
import org.scalatest.funspec.AnyFunSpec

class FieldTest extends AnyFunSpec with VerificationTools {

  describe(classOf[Field].getSimpleName) {

    it("should convert a TableColumn into a Field") {
      implicit val scope: Scope = Scope()
      val column = TableColumn(name = "symbol", `type` = StringType, defaultValue = Some("AMD".v))
      val field = column.toField()
      assert(field == Field(name = "symbol", metadata = FieldMetadata(), value = Some("AMD")))
    }

    it("should convert a TableColumn into a Field with a value") {
      val column = TableColumn(name = "symbol", `type` = StringType, defaultValue = Some("XXX".v))
      val field = column.withValue(value = Some("AAPL"))
      assert(field == Field(name = "symbol", metadata = FieldMetadata(), value = Some("AAPL")))
    }

  }

}
