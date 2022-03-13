package modules.mx.gui.userAlerts

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.paint.Color
import modules.mx.rightButtonsWidth
import tornadofx.Fragment
import tornadofx.action
import tornadofx.button
import tornadofx.form
import tornadofx.hbox
import tornadofx.label
import tornadofx.paddingTop
import tornadofx.paddingVertical
import tornadofx.separator
import tornadofx.style

class GAlert(message: String, askConfirm: Boolean = false) : Fragment() {
  val confirmed = SimpleBooleanProperty(false)
  override val root = form {
    requestFocus()
    label(message)
    if (askConfirm) {
      separator(Orientation.HORIZONTAL) {
        paddingVertical = 50
      }
      label("Continue?")
      hbox {
        paddingTop = 40
        alignment = Pos.CENTER
        button("Confirm") {
          action {
            confirmed.value = true
            close()
          }
          prefWidth = rightButtonsWidth
          style { unsafe("-fx-base", Color.DARKGREEN) }
        }
        button("Cancel") {
          action {
            confirmed.value = false
            close()
          }
          prefWidth = rightButtonsWidth
        }
      }
    }
  }
}
