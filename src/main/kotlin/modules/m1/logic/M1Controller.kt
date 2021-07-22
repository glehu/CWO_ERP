package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.gui.MG1Analytics
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.gui.MG1SongFinder
import modules.m1.misc.SongModel
import modules.m1.misc.SongProperty
import modules.m1.misc.getSongFromProperty
import modules.m2.Contact
import modules.m2.gui.MG2ContactFinder
import modules.m2.logic.M2DBManager
import modules.m2.logic.M2IndexManager
import tornadofx.Controller
import tornadofx.Scope

@ExperimentalSerializationApi
class M1Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M1Controller"
    override fun module() = "M1"

    private val wizard = find<SongConfiguratorWizard>()
    val db: CwODB by inject()
    val indexManager: M1IndexManager by inject(Scope(db))
    private val m2indexManager: M2IndexManager by inject(Scope(db))

    fun openWizardNewSong()
    {
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

    fun openWizardFindSong()
    {
        tornadofx.find(MG1SongFinder::class, Scope(indexManager)).openModal()
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        tornadofx.find(MG1Analytics::class, Scope(indexManager)).openModal()
    }
}