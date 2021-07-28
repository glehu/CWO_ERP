package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.Song
import modules.m1.gui.MG1Analytics
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.gui.SongViewerWizard
import modules.m1.misc.SongProperty
import modules.m1.misc.getSongFromProperty
import modules.m1.misc.getSongPropertyFromSong
import modules.mx.m1GlobalIndex
import tornadofx.Controller

@ExperimentalSerializationApi
class M1Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M1Controller"
    override fun module() = "M1"

    val db: CwODB by inject()

    fun openWizardNewSong()
    {
        val wizard = find<SongConfiguratorWizard>()
        wizard.song.item = SongProperty()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.song.item.nameProperty.value !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M1DBManager().saveEntry(
                    getSongFromProperty(wizard.song.item), db, -1L, -1, raf, m1GlobalIndex
                )
                db.closeRandomFileAccess(raf)
                wizard.song.item = SongProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        find<MG1Analytics>().openModal()
    }

    fun showSong(song: Song)
    {
        val wizard = find<SongViewerWizard>()
        wizard.song.item = getSongPropertyFromSong(song)
        wizard.onComplete {
            if (wizard.song.uID.value != -1)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M1DBManager().saveEntry(
                    entry = getSongFromProperty(wizard.song.item),
                    cwodb = db,
                    posDB = m1GlobalIndex.indexList[0]!!.indexMap[wizard.song.item.uID]!!.pos,
                    byteSize = m1GlobalIndex.indexList[0]!!.indexMap[wizard.song.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = m1GlobalIndex
                )
                db.closeRandomFileAccess(raf)
                wizard.song.item = SongProperty()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal()
    }
}