package modules

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.Stylesheet
import tornadofx.c
import tornadofx.derive

class Stylesheet : Stylesheet()
{
    init
    {
        root {
            accentColor = c("#1e74c6")
            focusColor = accentColor
            baseColor = c("#373e43")
            unsafe("-fx-control-inner-background", baseColor.derive(0.25))
            unsafe("-fx-control-inner-background-alt", raw("-fx-control-inner-background"))
        }

        label {
            textFill = Color.LIGHTGRAY
        }

        textField {
            promptTextFill = Color.GRAY
        }

        title {
            fontWeight = FontWeight.BOLD
        }

        button {
            focusTraversable = false
            and(":hover") {
                textFill = Color.WHITE
            }
            and(":default") {
                baseColor = accentColor
            }
        }

        ".separator *.line" {
            backgroundColor += c("#3C3C3C")
            unsafe("-fx-border-style", raw("solid"))
            unsafe("-fx-border-width", raw("1px"))
        }

        scrollBar {
            backgroundColor += baseColor.derive(0.45)
        }

        ".table-view" {
            unsafe("-fx-selection-bar-non-focused", baseColor.derive(0.5))
        }

        ".table-view .column-header .label" {
            unsafe("-fx-alignment", raw("CENTER_LEFT"))
            unsafe("-fx-font-weight", raw("none"))
        }

        s(listCell, tableRowCell) {
            and(even, odd) {
                unsafe("-fx-control-inner-background", c("#292d30"))
            }
        }

        s(listCell, tableRowCell) {
            and(empty) {
                unsafe("-fx-background-color", raw("transparent"))
            }
        }

        s(listCell, tableRowCell)
        {
            unsafe("-fx-border-color", raw("transparent"))
            unsafe("-fx-table-cell-border-color", raw("transparent"))
        }
    }
}