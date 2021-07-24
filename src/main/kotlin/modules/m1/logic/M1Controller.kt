package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.gui.MG1Analytics
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.misc.SongProperty
import modules.m1.misc.getSongFromProperty
import modules.m2.logic.M2IndexManager
import tornadofx.Controller
import tornadofx.Scope
import tornadofx.find

@ExperimentalSerializationApi
class M1Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M1Controller"
    override fun module() = "M1"

    val db: CwODB by inject()

    fun openWizardNewSong(indexManager: M1IndexManager, m2IndexManager: M2IndexManager)
    {
        val wizard = find<SongConfiguratorWizard>(Scope(m2IndexManager))
        wizard.song.item = SongProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.song.item.nameProperty.value !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M1DBManager().saveEntry(
                    getSongFromProperty(wizard.song.item), db, -1L, -1, raf, indexManager
                )
                db.closeRandomFileAccess(raf)
                wizard.song.item = SongProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }

    fun openAnalytics(indexManager: M1IndexManager)
    {
        //TODO: Add multiple analytics modes
        find(MG1Analytics::class, Scope(indexManager)).openModal()
    }
}