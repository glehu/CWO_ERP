package modules.mx.gui

import javafx.scene.image.Image
import tornadofx.Fragment
import tornadofx.form
import tornadofx.imageview
import tornadofx.toProperty

class GImageViewer(image: Image) : Fragment("Image Viewer") {
  override val root = form {
    setPrefSize(750.0, 750.0)
    add(imageview(image) {
      fitHeightProperty().bind(parent.prefHeight(750.0).toProperty())
      fitWidthProperty().bind(parent.prefWidth(750.0).toProperty())
    })
  }
}
