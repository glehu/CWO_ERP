package modules.m4.logic

import api.logic.getCWOClient
import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m4.M4Item
import modules.m4.gui.M4ItemConfiguratorWizard
import modules.m4.gui.MG4ItemFinder
import modules.m4.misc.M4ItemProperty
import modules.m4.misc.getM4ItemFromItemProperty
import modules.m4.misc.getM4ItemPropertyFromItem
import modules.mx.activeUser
import modules.mx.m4GlobalIndex
import tornadofx.Controller

@InternalAPI
@ExperimentalSerializationApi
class M4Controller : IController, Controller() {
    override val moduleNameLong = "M4Controller"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return m4GlobalIndex
    }

    private val wizard = find<M4ItemConfiguratorWizard>()
    val client = getCWOClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG4ItemFinder>().openModal()
    }

    override fun newEntry() {
        wizard.item.item = M4ItemProperty()
        wizard.item.validate()
        wizard.isComplete = false
    }

    override fun saveEntry() {
        if (wizard.item.isValid) {
            wizard.item.commit()
            wizard.item.uID.value = save(getM4ItemFromItemProperty(wizard.item.item))
            wizard.isComplete = false
        }
    }

    override fun showEntry(uID: Int) {
        val entry = get(uID) as M4Item
        wizard.item.item = getM4ItemPropertyFromItem(entry)
    }

    fun createAndReturnItem(): M4Item {
        val item = M4Item(-1, "")
        val wizard = M4ItemConfiguratorWizard()
        wizard.showHeader = false
        wizard.showSteps = false
        wizard.item.item = getM4ItemPropertyFromItem(item)
        wizard.openModal(block = true)
        return getM4ItemFromItemProperty(wizard.item.item)
    }
}