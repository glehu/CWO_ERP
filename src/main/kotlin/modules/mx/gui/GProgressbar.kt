package modules.mx.gui

import javafx.scene.text.FontWeight
import tornadofx.TaskStatus
import tornadofx.View
import tornadofx.label
import tornadofx.progressbar
import tornadofx.style
import tornadofx.vbox
import tornadofx.visibleWhen

class GProgressbar : View() {
  private val status: TaskStatus by inject()

  override val root = vbox {
    visibleWhen { status.running }
    label(status.title).style { fontWeight = FontWeight.BOLD }
    vbox(4) {
      label(status.message)
      progressbar(status.progress)
      visibleWhen { status.running }
    }
  }
}
