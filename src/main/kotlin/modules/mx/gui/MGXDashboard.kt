package modules.mx.gui

import kotlinx.serialization.ExperimentalSerializationApi
import tornadofx.View
import tornadofx.fieldset
import tornadofx.form

@ExperimentalSerializationApi
class MGXDashboard : View("Dashboard") {
    private val dbManager = find<MGXDatabaseManager>()
    override val root = form {
        fieldset(dbManager.title) {
            add(dbManager.table)
        }
    }
}