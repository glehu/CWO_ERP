package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.Song
import modules.m1.gui.MG1Analytics
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.gui.SongViewerWizard
import modules.m1.misc.*
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
        wizard.songP1.item = SongPropertyP1()
        wizard.songP2.item = SongPropertyP2()
        wizard.isComplete = false
        wizard.onComplete {
            if (wizard.songP1.item.nameProperty.value !== null)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M1DBManager().saveEntry(
                    getSongFromPropertyP2(
                        getSongFromPropertyP1(wizard.songP1.item),
                        wizard.songP2.item
                    ), db, -1L, -1, raf, m1GlobalIndex
                )
                db.closeRandomFileAccess(raf)
                wizard.songP1.item = SongPropertyP1()
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal(block = true)
    }

    fun openAnalytics()
    {
        //TODO: Add multiple analytics modes
        find<MG1Analytics>().openModal()
    }

    fun showSong(song: Song)
    {
        val wizard = find<SongViewerWizard>()
        wizard.songP1.item = getSongPropertyP1FromSong(song)
        wizard.songP2.item = getSongPropertyP2FromSong(song)
        wizard.onComplete {
            if (wizard.songP1.uID.value != -1)
            {
                val raf = db.openRandomFileAccess(module(), "rw")
                M1DBManager().saveEntry(
                    entry = getSongFromPropertyP2(
                        getSongFromPropertyP1(wizard.songP1.item),
                        wizard.songP2.item
                    ),
                    cwodb = db,
                    posDB = m1GlobalIndex.indexList[0]!!.indexMap[wizard.songP1.item.uID]!!.pos,
                    byteSize = m1GlobalIndex.indexList[0]!!.indexMap[wizard.songP1.item.uID]!!.byteSize,
                    raf = raf,
                    indexManager = m1GlobalIndex
                )
                db.closeRandomFileAccess(raf)
                wizard.songP1.item = getSongPropertyP1FromSong(song)
                wizard.songP2.item = getSongPropertyP2FromSong(song)
                wizard.isComplete = false
                wizard.close()
            }
        }
        wizard.openModal(block = true)
    }
}