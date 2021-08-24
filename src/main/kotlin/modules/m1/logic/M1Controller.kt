package modules.m1.logic

import api.misc.json.M1EntryListJson
import db.CwODB
import interfaces.IModule
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.gui.MG1Analytics
import modules.m1.gui.MG1EntryFinder
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.misc.*
import modules.m2.logic.M2Controller
import modules.mx.isClientGlobal
import modules.mx.m1GlobalIndex
import modules.mx.maxSearchResultsGlobal
import tornadofx.Controller
import tornadofx.Scope

@ExperimentalSerializationApi
class M1Controller : IModule, Controller() {
    override fun moduleNameLong() = "M1Controller"
    override fun module() = "M1"

    val db: CwODB by inject()
    private val m2Controller: M2Controller by inject()

    fun searchEntry() {
        find<MG1EntryFinder>().openModal()
    }

    fun saveEntry() {
        val wizard = find<SongConfiguratorWizard>()
        var isComplete = true
        if (!wizard.songMainData.isValid) isComplete = false
        if (isComplete) {
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
            val raf = db.openRandomFileAccess(module(), CwODB.RafMode.READWRITE)
            wizard.songMainData.uID.value = M1DBManager().saveEntry(
                entry = getSongFromProperties(wizard),
                cwodb = db,
                posDB = -1L,
                byteSize = -1,
                raf = raf,
                indexManager = m1GlobalIndex
            )
            db.closeRandomFileAccess(raf)
            wizard.isComplete = false
        } else wizard.songMainData.validate()
    }

    fun newEntry() {
        val wizard = find<SongConfiguratorWizard>()
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

    fun openAnalytics() {
        //TODO: Add multiple analytics modes
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
            M1DBManager().getEntry(
                dataTransfer.uID.value, db, m1GlobalIndex.indexList[0]!!
            ) as Song
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

    fun showSong(song: Song) {
        val wizard = find<SongConfiguratorWizard>()
        wizard.songMainData.item = getSongPropertyMainData(song)
        wizard.songCompletionState.item = getSongPropertyCompletionState(song)
        wizard.songPromotionData.item = getSongPropertyPromotionData(song)
        wizard.songFinancialData.item = getSongPropertyFinancialData(song)
        wizard.songAvailabilityData.item = getSongPropertyAvailabilityData(song)
        wizard.songVisualizationData.item = getSongPropertyVisualizationData(song)
        wizard.songAlbumEPData.item = getSongPropertyAlbumEPData(song)
        wizard.songStatisticsData.item = getSongPropertyStatisticsData(song)
        wizard.songCollaborationData.item = getSongPropertyCollaborationData(song)
        wizard.songCopyrightData.item = getSongPropertyCopyrightData(song)
        wizard.songMiscData.item = getSongPropertyMiscData(song)

        if (!isClientGlobal) {
            //Sync album data
            val album = getEntry(wizard.songAlbumEPData.item.albumUID)
            if (album.uID != -1) {
                wizard.songAlbumEPData.item.nameAlbum = album.name
                wizard.songAlbumEPData.item.typeAlbum = album.type
            }

            //Sync contact data
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

        wizard.onComplete {
            saveEntry()
        }
    }

    private fun getEntryName(uID: Int, default: String): String {
        return if (uID != -1) {
            getEntry(uID).name
        } else default
    }

    fun getEntry(uID: Int): Song {
        return if (uID != -1) {
            M1DBManager().getEntry(
                uID, db, m1GlobalIndex.indexList[0]!!
            ) as Song
        } else Song(-1, "")
    }

    fun getEntryListJson(searchText: String, ixNr: Int): M1EntryListJson {
        val resultsListJson = M1EntryListJson(0, arrayListOf())
        var resultCounter = 0
        db.getEntriesFromSearchString(
            searchText = searchText.uppercase(),
            ixNr = ixNr,
            exactSearch = false,
            module = module(),
            maxSearchResults = maxSearchResultsGlobal,
            indexManager = m1GlobalIndex
        ) { _, bytes ->
            resultCounter++
            resultsListJson.resultsList.add(bytes)
        }
        resultsListJson.resultsAmount = resultCounter
        return resultsListJson
    }
}