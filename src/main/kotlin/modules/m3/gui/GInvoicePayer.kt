package modules.m3.gui

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.paint.Color
import modules.mx.rightButtonsWidth
import tornadofx.Fragment
import tornadofx.action
import tornadofx.button
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.hbox
import tornadofx.label
import tornadofx.style
import tornadofx.textfield

class GInvoicePayer : Fragment("Pay Invoice") {
  var userConfirmed = false
  private val amountToPay = SimpleDoubleProperty(0.0)
  val paidAmount = SimpleDoubleProperty(0.0)
  override val root = form {
    fieldset("Payment") {
      field("Total") {
        hbox(4) {
          label(amountToPay)
          label("EUR")
        }
      }
      field("Amount") { textfield(paidAmount) }
    }
    button("Pay (Enter)") {
      shortcut("Enter")
      action {
        userConfirmed = true
        close()
      }
      prefWidth = rightButtonsWidth
    }
    button("Pay Fully (CTRL-S)") {
      shortcut("CTRL-S")
      action {
        paidAmount.value = amountToPay.value
        userConfirmed = true
        close()
      }
      prefWidth = rightButtonsWidth
      style { unsafe("-fx-base", Color.DARKGREEN) }
    }
  }

  fun setAmountToPay(amountToPay: Double) {
    this.amountToPay.value = amountToPay
  }
}
