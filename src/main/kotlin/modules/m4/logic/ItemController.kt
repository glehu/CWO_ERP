package modules.m4.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.misc.SongPropertyMainDataModel
import modules.m4.Item
import modules.m4.gui.M4ItemConfiguratorWizard
import modules.m4.gui.GItemFinder
import modules.m4.gui.GItemSettings
import modules.m4.misc.M4ItemProperty
import modules.m4.misc.getM4ItemFromItemProperty
import modules.m4.misc.getM4ItemPropertyFromItem
import modules.mx.m4GlobalIndex
import tornadofx.Controller
import tornadofx.Scope

@InternalAPI
@ExperimentalSerializationApi
class ItemController : IController, Controller() {
    override val moduleNameLong = "M4Controller"
    override val module = "M4"
    override fun getIndexManager(): IIndexManager {
        return m4GlobalIndex!!
    }

    private val wizard = find<M4ItemConfiguratorWizard>()

    override fun searchEntry() {
        find<GItemFinder>().openModal()
    }

    override fun newEntry() {
        wizard.item.commit()
        if (wizard.item.isValid && wizard.item.uID.value != -1) {
            setEntryLock(wizard.item.uID.value, false)
        }
        wizard.item.priceCategories.value.clear()
        wizard.item.storages.value.clear()
        wizard.item.item = M4ItemProperty()
        wizard.item.validate()
        wizard.isComplete = false
    }

    override suspend fun saveEntry(unlock: Boolean) {
        if (wizard.item.isValid) {
            wizard.item.commit()
            wizard.item.uID.value = save(
                entry = getM4ItemFromItemProperty(wizard.item.item),
                unlock = unlock
            )
            wizard.isComplete = false
        }
    }

    override fun showEntry(uID: Int) {
        val entry = get(uID) as Item
        wizard.item.item = getM4ItemPropertyFromItem(entry)
    }

    fun createAndReturnItem(): Item {
        val item = Item(-1, "")
        val wizard = M4ItemConfiguratorWizard()
        wizard.showHeader = false
        wizard.showSteps = false
        wizard.item.item = getM4ItemPropertyFromItem(item)
        wizard.openModal(block = true)
        return getM4ItemFromItemProperty(wizard.item.item)
    }

    fun selectAndReturnItem(): Item {
        val item: Item
        val newScope = Scope()
        val dataTransfer = SongPropertyMainDataModel()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        tornadofx.find<GItemFinder>(newScope).openModal(block = true)
        item = if (dataTransfer.name.value != null) {
            load(dataTransfer.uID.value) as Item
        } else Item(-1, "")
        return item
    }

    fun showSettings() {
        val settings = find<GItemSettings>()
        settings.openModal()
    }
}
