package modules.m1.logic

import api.logic.getCWOClient
import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.gui.MG1Analytics
import modules.m1.gui.MG1EntryFinder
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.misc.*
import modules.m2.logic.M2Controller
import modules.mx.activeUser
import modules.mx.m1GlobalIndex
import tornadofx.Controller
import tornadofx.Scope

@InternalAPI
@ExperimentalSerializationApi
class M1Controller : IController, Controller() {
    override val moduleNameLong = "M1Controller"
    override val module = "M1"
    override fun getIndexManager(): IIndexManager {
        return m1GlobalIndex
    }

    private val m2Controller: M2Controller by inject()
    private val wizard = find<SongConfiguratorWizard>()
    val client = getCWOClient(activeUser.username, activeUser.password)

    override fun searchEntry() {
        find<MG1EntryFinder>().openModal()
    }

    override fun saveEntry() {
        if (wizard.songMainData.isValid) {
            wizard.songMainData.commit()
            wizard.songCompletionState.commit()
            wizard.songPromotionData.commit()
            wizard.songFinancialData.commit()
            wizard.songAvailabilityData.commit()
            wizard.songVisualizationData.commit()
            wizard.songAlbumEPData.commit()
            wizard.songStatisticsData.commit()
            wizard.songCollaborationData.commit()
            wizard.songCopyrightData.commit()
            wizard.songMiscData.commit()
            wizard.songMainData.uID.value = save(getSongFromProperties(wizard))
            wizard.isComplete = false
        } else wizard.songMainData.validate()
    }

    override fun newEntry() {
        wizard.songMainData.item = SongPropertyMainData()
        wizard.songCompletionState.item = SongPropertyCompletionState()
        wizard.songPromotionData.item = SongPropertyPromotionData()
        wizard.songFinancialData.item = SongPropertyFinancialData()
        wizard.songAvailabilityData.item = SongPropertyAvailabilityData()
        wizard.songVisualizationData.item = SongPropertyVisualizationData()
        wizard.songAlbumEPData.item = SongPropertyAlbumEPData()
        wizard.songStatisticsData.item = SongPropertyStatisticsData()
        wizard.songCollaborationData.item = SongPropertyCollaborationData()
        wizard.songCopyrightData.item = SongPropertyCopyrightData()
        wizard.songMiscData.item = SongPropertyMiscData()

        wizard.songMainData.type.value = getSongTypeList()[0]
        wizard.songMainData.validate()
        wizard.isComplete = false
    }

    override fun showEntry(uID: Int) {
        val entry = get(uID) as Song
        wizard.songMainData.item = getSongPropertyMainData(entry)
        wizard.songCompletionState.item = getSongPropertyCompletionState(entry)
        wizard.songPromotionData.item = getSongPropertyPromotionData(entry)
        wizard.songFinancialData.item = getSongPropertyFinancialData(entry)
        wizard.songAvailabilityData.item = getSongPropertyAvailabilityData(entry)
        wizard.songVisualizationData.item = getSongPropertyVisualizationData(entry)
        wizard.songAlbumEPData.item = getSongPropertyAlbumEPData(entry)
        wizard.songStatisticsData.item = getSongPropertyStatisticsData(entry)
        wizard.songCollaborationData.item = getSongPropertyCollaborationData(entry)
        wizard.songCopyrightData.item = getSongPropertyCopyrightData(entry)
        wizard.songMiscData.item = getSongPropertyMiscData(entry)

        //Sync album data
        if (wizard.songAlbumEPData.item.albumUID != -1) {
            val album = load(wizard.songAlbumEPData.item.albumUID) as Song
            wizard.songAlbumEPData.item.nameAlbum = album.name
            wizard.songAlbumEPData.item.typeAlbum = album.type
        }

        //Sync contact data
        wizard.songMainData.item.vocalist =
            m2Controller.getContactName(
                wizard.songMainData.item.vocalistUID, wizard.songMainData.item.vocalist
            )
        wizard.songMainData.item.producer =
            m2Controller.getContactName(
                wizard.songMainData.item.producerUID, wizard.songMainData.item.producer
            )
        wizard.songMainData.item.mixing =
            m2Controller.getContactName(
                wizard.songMainData.item.mixingUID, wizard.songMainData.item.mixing
            )
        wizard.songMainData.item.mastering =
            m2Controller.getContactName(
                wizard.songMainData.item.masteringUID, wizard.songMainData.item.mastering
            )
        wizard.songCollaborationData.item.coVocalist1 =
            m2Controller.getContactName(
                wizard.songCollaborationData.item.coVocalist1UID, wizard.songCollaborationData.item.coVocalist1
            )
        wizard.songCollaborationData.item.coVocalist2 =
            m2Controller.getContactName(
                wizard.songCollaborationData.item.coVocalist2UID, wizard.songCollaborationData.item.coVocalist2
            )
        wizard.songCollaborationData.item.coProducer1 =
            m2Controller.getContactName(
                wizard.songCollaborationData.item.coProducer1UID, wizard.songCollaborationData.item.coProducer1
            )
        wizard.songCollaborationData.item.coProducer2 =
            m2Controller.getContactName(
                wizard.songCollaborationData.item.coProducer2UID, wizard.songCollaborationData.item.coProducer2
            )
    }

    fun openAnalytics() {
        find<MG1Analytics>().openModal()
    }

    fun selectAndReturnEntry(): Song {
        val entry: Song
        val newScope = Scope()
        val dataTransfer = SongPropertyMainDataModel()
        dataTransfer.uID.value = -2
        setInScope(dataTransfer, newScope)
        tornadofx.find<MG1EntryFinder>(newScope).openModal(block = true)
        entry = if (dataTransfer.name.value != null) {
            load(dataTransfer.uID.value) as Song
        } else Song(-1, "")
        return entry
    }

    private fun getSongFromProperties(wizard: SongConfiguratorWizard): Song {
        var song = Song(-1, "")
        song = getSongFromProperty(song, wizard.songMainData.item)
        song = getSongFromProperty(song, wizard.songCompletionState.item)
        song = getSongFromProperty(song, wizard.songPromotionData.item)
        song = getSongFromProperty(song, wizard.songFinancialData.item)
        song = getSongFromProperty(song, wizard.songAvailabilityData.item)
        song = getSongFromProperty(song, wizard.songVisualizationData.item)
        song = getSongFromProperty(song, wizard.songAlbumEPData.item)
        song = getSongFromProperty(song, wizard.songStatisticsData.item)
        song = getSongFromProperty(song, wizard.songCollaborationData.item)
        song = getSongFromProperty(song, wizard.songCopyrightData.item)
        song = getSongFromProperty(song, wizard.songMiscData.item)
        return song
    }
}