package modules.mx.gui.userAlerts

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import modules.mx.rightButtonsWidth
import tornadofx.*

class GAlert(message: String, askConfirm: Boolean = false) : Fragment() {
    val confirmed = SimpleBooleanProperty(false)
    override val root = form {
        requestFocus()
        label(message)
        if (askConfirm) {
            hbox {
                paddingTop = 50
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
