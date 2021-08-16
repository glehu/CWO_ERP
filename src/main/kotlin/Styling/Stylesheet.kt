package Styling

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*
import tornadofx.Stylesheet

class Stylesheet : Stylesheet()
{
    companion object {
        val fieldsetBorder by cssclass()
    }
    init
    {
        root {
            unsafe("-fx-accent", Color.web("#1e74c6", 1.0))
            unsafe("-fx-base", Color.web("#373e43", 1.0))
            unsafe("-fx-focus-color", Color.web("#1e74c6", 1.0))
            unsafe("-fx-control-inner-background", Color.web("#373e43", 1.0).derive(0.25))
            unsafe("-fx-control-inner-background-alt", raw("-fx-control-inner-background"))
        }

        label {
            unsafe("-fx-text-fill", Color.LIGHTGRAY)
        }

        fieldsetBorder {
            unsafe("-fx-border-color", Color.GRAY)
            //unsafe("-fx-background-color", Color.web("#3C3C3C", 1.0))
            unsafe("-fx-background-color", Color.web("#373e43", 1.0))
            padding = CssBox(
                Dimension(5.0, Dimension.LinearUnits.px),
                Dimension(5.0, Dimension.LinearUnits.px),
                Dimension(5.0, Dimension.LinearUnits.px),
                Dimension(5.0, Dimension.LinearUnits.px)
            )
        }

        textField {
            unsafe("-fx-prompt-text-fill", Color.GRAY)
            unsafe("-fx-text-fill", Color.WHITE)
            unsafe("-fx-font-weight", FontWeight.EXTRA_BOLD)
        }

        title {
            unsafe("-fx-font-weight", FontWeight.BOLD)
        }

        button {
            focusTraversable = false
            and(":hover") {
                unsafe("-fx-text-fill", Color.WHITE)
            }
            and(":default") {
                unsafe("-fx-base", Color.web("#1e74c6", 1.0))
            }
        }

        ".separator *.line" {
            unsafe("-fx-background-color", Color.web("#3C3C3C", 1.0))
            unsafe("-fx-border-style", raw("solid"))
            unsafe("-fx-border-width", raw("1px"))
        }

        scrollBar {
            unsafe("-fx-background-color", Color.web("#373e43").derive(0.45))
        }

        ".table-view" {
            unsafe("-fx-selection-bar-non-focused", c("#373e43").derive(0.5))
        }

        ".table-view .column-header .label" {
            unsafe("-fx-alignment", raw("CENTER_LEFT"))
            unsafe("-fx-font-weight", raw("none"))
        }

        s(listCell, tableRowCell) {
            and(even) {
                unsafe("-fx-control-inner-background", c("#292d30"))
            }
            and(odd) {
                unsafe("-fx-control-inner-background", c("#32373b"))
            }
        }

        s(listCell, tableRowCell) {
            unsafe("-fx-table-cell-border-color", raw("transparent"))
            and(empty) {
                unsafe("-fx-background-color", raw("transparent"))
            }
        }
    }
}