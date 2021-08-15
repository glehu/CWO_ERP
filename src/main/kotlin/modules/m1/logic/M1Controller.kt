package modules.m1.logic

import db.CwODB
import kotlinx.serialization.ExperimentalSerializationApi
import modules.IModule
import modules.m1.Song
import modules.m1.gui.MG1Analytics
import modules.m1.gui.MG1SongFinder
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.gui.SongViewerWizard
import modules.m1.misc.*
import modules.m2.logic.M2Controller
import modules.mx.m1GlobalIndex
import tornadofx.Controller

@ExperimentalSerializationApi
class M1Controller : IModule, Controller()
{
    override fun moduleNameLong() = "M1Controller"
    override fun module() = "M1"

    val db: CwODB by inject()
    private val m2Controller: M2Controller by inject()

    fun openSearchScreen()
    {
        find<MG1SongFinder>().openModal()
    }

    fun saveSong()
    {
        val wizard = find<SongConfiguratorWizard>()
        var isComplete = true
        wizard.songP1.commit()
        wizard.songP2.commit()
        if(!wizard.songP1.isValid) isComplete = false
        if(!wizard.songP2.isValid) isComplete = false
        if (isComplete)
        {
            val raf = db.openRandomFileAccess(module(), "rw")
            M1DBManager().saveEntry(
                getSongFromPropertyP2(
                    getSongFromPropertyP1(wizard.songP1.item),
                    wizard.songP2.item
                ), db, -1L, -1, raf, m1GlobalIndex
            )
            db.closeRandomFileAccess(raf)
            wizard.isComplete = false
        }
    }

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
                wizard.songP2.item = SongPropertyP2()
                wizard.isComplete = false
                //wizard.close()
            }
        }
        //wizard.openModal(block = true)
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

        //Sync contact data
        wizard.songP1.item.mixing =
            m2Controller.getContactName(wizard.songP1.item.mixingUID, wizard.songP1.item.mixing)
        wizard.songP1.item.mastering =
            m2Controller.getContactName(wizard.songP1.item.masteringUID, wizard.songP1.item.mastering)
        wizard.songP2.item.coVocalist1 =
            m2Controller.getContactName(wizard.songP2.item.coVocalist1UID, wizard.songP2.item.coVocalist1)
        wizard.songP2.item.coVocalist2 =
            m2Controller.getContactName(wizard.songP2.item.coVocalist2UID, wizard.songP2.item.coVocalist2)
        wizard.songP2.item.coProducer1 =
            m2Controller.getContactName(wizard.songP2.item.coProducer1UID, wizard.songP2.item.coProducer1)
        wizard.songP2.item.coProducer2 =
            m2Controller.getContactName(wizard.songP2.item.coProducer2UID, wizard.songP2.item.coProducer2)

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
                wizard.songP1.item = SongPropertyP1()
                wizard.songP2.item = SongPropertyP2()
                wizard.isComplete = false
                //wizard.close()
            }
        }
        //wizard.openModal(block = true)
    }
}