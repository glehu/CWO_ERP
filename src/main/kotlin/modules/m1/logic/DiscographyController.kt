package modules.m1.logic

import interfaces.IController
import interfaces.IIndexManager
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.Song
import modules.m1.gui.GDiscographyAnalytics
import modules.m1.gui.GDiscographyFinder
import modules.m1.gui.SongConfiguratorWizard
import modules.m1.misc.SongPropertyAlbumEPData
import modules.m1.misc.SongPropertyAvailabilityData
import modules.m1.misc.SongPropertyCollaborationData
import modules.m1.misc.SongPropertyCompletionState
import modules.m1.misc.SongPropertyCopyrightData
import modules.m1.misc.SongPropertyFinancialData
import modules.m1.misc.SongPropertyMainData
import modules.m1.misc.SongPropertyMainDataModel
import modules.m1.misc.SongPropertyMiscData
import modules.m1.misc.SongPropertyPromotionData
import modules.m1.misc.SongPropertyStatisticsData
import modules.m1.misc.SongPropertyVisualizationData
import modules.m1.misc.getSongFromProperty
import modules.m1.misc.getSongPropertyAlbumEPData
import modules.m1.misc.getSongPropertyAvailabilityData
import modules.m1.misc.getSongPropertyCollaborationData
import modules.m1.misc.getSongPropertyCompletionState
import modules.m1.misc.getSongPropertyCopyrightData
import modules.m1.misc.getSongPropertyFinancialData
import modules.m1.misc.getSongPropertyMainData
import modules.m1.misc.getSongPropertyMiscData
import modules.m1.misc.getSongPropertyPromotionData
import modules.m1.misc.getSongPropertyStatisticsData
import modules.m1.misc.getSongPropertyVisualizationData
import modules.m1.misc.getSongTypeList
import modules.m2.logic.ContactController
import modules.mx.discographyIndexManager
import tornadofx.Controller
import tornadofx.Scope

@InternalAPI
@ExperimentalSerializationApi
class DiscographyController : IController, Controller() {
  override val moduleNameLong = "DiscographyController"
  override val module = "M1"
  override fun getIndexManager(): IIndexManager {
    return discographyIndexManager!!
  }

  private val contactController: ContactController by inject()
  private val wizard = find<SongConfiguratorWizard>()

  override fun searchEntry() {
    find<GDiscographyFinder>().openModal()
  }

  override suspend fun saveEntry(unlock: Boolean) {
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
      wizard.songMainData.uID.value = save(getSongFromProperties(wizard), unlock = unlock)
      wizard.isComplete = false
    } else wizard.songMainData.validate()
  }

  override fun newEntry() {
    wizard.songMainData.commit()
    if (wizard.songMainData.isValid && wizard.songMainData.uID.value != -1) {
      setEntryLock(wizard.songMainData.uID.value, false)
    }
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
      contactController.getContactName(
        wizard.songMainData.item.vocalistUID, wizard.songMainData.item.vocalist
      )
    wizard.songMainData.item.producer =
      contactController.getContactName(
        wizard.songMainData.item.producerUID, wizard.songMainData.item.producer
      )
    wizard.songMainData.item.mixing =
      contactController.getContactName(
        wizard.songMainData.item.mixingUID, wizard.songMainData.item.mixing
      )
    wizard.songMainData.item.mastering =
      contactController.getContactName(
        wizard.songMainData.item.masteringUID, wizard.songMainData.item.mastering
      )
    wizard.songCollaborationData.item.coVocalist1 =
      contactController.getContactName(
        wizard.songCollaborationData.item.coVocalist1UID, wizard.songCollaborationData.item.coVocalist1
      )
    wizard.songCollaborationData.item.coVocalist2 =
      contactController.getContactName(
        wizard.songCollaborationData.item.coVocalist2UID, wizard.songCollaborationData.item.coVocalist2
      )
    wizard.songCollaborationData.item.coProducer1 =
      contactController.getContactName(
        wizard.songCollaborationData.item.coProducer1UID, wizard.songCollaborationData.item.coProducer1
      )
    wizard.songCollaborationData.item.coProducer2 =
      contactController.getContactName(
        wizard.songCollaborationData.item.coProducer2UID, wizard.songCollaborationData.item.coProducer2
      )
  }

  fun openAnalytics() {
    find<GDiscographyAnalytics>().openModal()
  }

  fun selectAndReturnEntry(): Song {
    val entry: Song
    val newScope = Scope()
    val dataTransfer = SongPropertyMainDataModel()
    dataTransfer.uID.value = -2
    setInScope(dataTransfer, newScope)
    tornadofx.find<GDiscographyFinder>(newScope).openModal(block = true)
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
