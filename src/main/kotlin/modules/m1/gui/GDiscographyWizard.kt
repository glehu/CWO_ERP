@file:Suppress(
  "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode",
  "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode"
)

package modules.m1.gui

import io.ktor.util.*
import javafx.collections.FXCollections
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.logic.DiscographyController
import modules.m1.misc.*
import modules.m2.logic.ContactController
import tornadofx.*

//This Wizard is used to create new songs
@InternalAPI
@ExperimentalSerializationApi
class SongConfiguratorWizard : Wizard("Add new entry") {
  val songMainData: SongPropertyMainDataModel by inject()
  val songCompletionState: SongPropertyCompletionStateModel by inject()
  val songPromotionData: SongPropertyPromotionDataModel by inject()
  val songFinancialData: SongPropertyFinancialDataModel by inject()
  val songAvailabilityData: SongPropertyAvailabilityDataModel by inject()
  val songVisualizationData: SongPropertyVisualizationDataModel by inject()
  val songAlbumEPData: SongPropertyAlbumEPDataModel by inject()
  val songStatisticsData: SongPropertyStatisticsDataModel by inject()
  val songCollaborationData: SongPropertyCollaborationDataModel by inject()
  val songCopyrightData: SongPropertyCopyrightDataModel by inject()
  val songMiscData: SongPropertyMiscDataModel by inject()

  init {
    enableStepLinks = true
    add(SongMainData::class)
    add(SongCompletionState::class)
    add(SongPromotionData::class)
    add(SongFinancialData::class)
    add(SongAvailabilityData::class)
    add(SongVisualizationData::class)
    add(SongAlbumEPData::class)
    add(SongStatisticsData::class)
    add(SongCollaborationData::class)
    add(SongCopyrightData::class)
    add(SongMiscData::class)
  }
}

@InternalAPI
@ExperimentalSerializationApi
class SongMainData : Fragment("Main") {
  private val contactController: ContactController by inject()

  private val songMainData: SongPropertyMainDataModel by inject()

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  private val genreList = FXCollections.observableArrayList(getGenreList())!!
  private val typeList = FXCollections.observableArrayList(getSongTypeList())!!

  override val root = form {
    fieldset {
      field("UID") {
        label(songMainData.uID)
      }
      field("Name") { textfield(songMainData.name).required() }
      field("Vocalist") {
        hbox {
          textfield(songMainData.vocalist) {
            contextmenu {
              item("Show contact").action {
                if (songMainData.vocalistUID.value != -1) {
                  contactController.showEntry(songMainData.vocalistUID.value)
                }
              }
              item("Load contact").action {
                val contact = contactController.selectAndLoadContact()
                songMainData.vocalistUID.value = contact.uID
                songMainData.vocalist.value = contact.name
              }
              item("Contact's Songs").action {
                val finder = GDiscographyFinder()
                finder.openModal()
                finder.modalSearch(songMainData.vocalist.value, 2)
              }
            }
          }
        }
      }
      field("Producer") {
        hbox {
          textfield(songMainData.producer) {
            contextmenu {
              item("Show contact").action {
                if (songMainData.producerUID.value != -1) contactController.showEntry(
                  songMainData.producerUID.value
                )
              }
              item("Load contact").action {
                val contact = contactController.selectAndLoadContact()
                songMainData.producerUID.value = contact.uID
                songMainData.producer.value = contact.name
              }
              item("Contact's Songs").action {
                val finder = GDiscographyFinder()
                finder.openModal()
                finder.modalSearch(songMainData.producer.value, 3)
              }
            }
          }
        }
      }
      field("Mixing") {
        hbox {
          textfield(songMainData.mixing) {
            contextmenu {
              item("Show contact").action {
                if (songMainData.mixingUID.value != -1) contactController.showEntry(
                  songMainData.mixingUID.value
                )
                songMainData.mixing.value =
                  contactController.getContactName(
                    songMainData.mixingUID.value,
                    songMainData.mixing.value
                  )
              }
              item("Load contact").action {
                val contact = contactController.selectAndLoadContact()
                songMainData.mixingUID.value = contact.uID
                songMainData.mixing.value = contact.name
              }
            }
          }
        }
      }
      field("Mastering") {
        hbox {
          textfield(songMainData.mastering) {
            contextmenu {
              item("Show contact").action {
                if (songMainData.masteringUID.value != -1) contactController.showEntry(
                  songMainData.masteringUID.value
                )
                songMainData.mastering.value =
                  contactController.getContactName(
                    songMainData.masteringUID.value,
                    songMainData.mastering.value
                  )
              }
              item("Load contact").action {
                val contact = contactController.selectAndLoadContact()
                songMainData.masteringUID.value = contact.uID
                songMainData.mastering.value = contact.name
              }
            }
          }
        }
      }
      field("Type") { combobox(songMainData.type, typeList) }
      field("Genre") { combobox(songMainData.genre, genreList) }
      field("Subgenre") { textfield(songMainData.subgenre) }
      field("Length") { textfield(songMainData.songLength) }
      field("Vibe") { textfield(songMainData.vibe) }
    }
  }

  override fun onSave() {
    isComplete = songMainData.commit()
  }
}

class SongCompletionState : Fragment("Completion State") {
  private val songCompletionState: SongPropertyCompletionStateModel by inject()

  //----------------------------------v
  //----------- Main Data ------------|
  //----------------------------------^
  private val songStateList = FXCollections.observableArrayList(
    "Instrumental", "Song"
  )!!
  private val instruStateList = FXCollections.observableArrayList(
    "Draft", "Polishing", "Finished"
  )!!
  private val lyricsStateList = FXCollections.observableArrayList(
    "Draft", "Polishing", "Finished"
  )!!
  private val vocalsStateList = FXCollections.observableArrayList(
    "Draft", "Polishing", "Finished"
  )!!
  private val mixingStateList = FXCollections.observableArrayList(
    "Basic", "Draft", "Polishing", "Finished"
  )!!
  private val masteringStateList = FXCollections.observableArrayList(
    "Basic", "Draft", "Polishing", "Finished"
  )!!

  override val root = form {
    fieldset {
      field("Song state") { combobox(songCompletionState.songState, songStateList) }
      field("Instrumental state") { combobox(songCompletionState.instruState, instruStateList) }
      field("Lyrics state") { combobox(songCompletionState.lyricsState, lyricsStateList) }
      field("Vocals state") { combobox(songCompletionState.vocalsState, vocalsStateList) }
      field("Mixing state") { combobox(songCompletionState.mixingState, mixingStateList) }
      field("Mastering state") { combobox(songCompletionState.masteringState, masteringStateList) }
    }
  }

  override fun onSave() {
    isComplete = songCompletionState.commit()
  }
}

class SongPromotionData : Fragment("Promotion") {
  private val songPromotionData: SongPropertyPromotionDataModel by inject()

  //----------------------------------v
  //--------- Promotion Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Promoted") { checkbox("", songPromotionData.isPromoted) }
      field("Distributed") { checkbox("", songPromotionData.distributed) }
      field("Exclusive Release") { checkbox("", songPromotionData.isExclusiveRelease) }
      field("Exclusive Channel") { textfield(songPromotionData.exclusiveChannel) }
    }
  }

  override fun onSave() {
    isComplete = songPromotionData.commit()
  }
}

class SongFinancialData : Fragment("Finances") {
  private val songFinancialData: SongPropertyFinancialDataModel by inject()

  //----------------------------------v
  //--------- Financial Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset("Money Spent") { field { textfield(songFinancialData.moneySpent) } }
    fieldset("Money Earned") {
      field("Streams") { textfield(songFinancialData.moneyGainedStreams) }
      field("Sponsoring") { textfield(songFinancialData.moneyGainedSponsor) }
    }
  }

  override fun onSave() {
    isComplete = songFinancialData.commit()
  }
}

class SongAvailabilityData : Fragment("Availability") {
  private val songAvailabilityData: SongPropertyAvailabilityDataModel by inject()

  //----------------------------------v
  //------- Availability Data --------|
  //----------------------------------^
  override val root = form {
    fieldset("Release") {
      field("Public") { checkbox("", songAvailabilityData.isPublic) }
      field("Date") { datepicker(songAvailabilityData.releaseDate) }
    }
    fieldset("Platforms") {
      field("Spotify") { checkbox("", songAvailabilityData.onSpotify) }
      field("YouTube") { checkbox("", songAvailabilityData.onYouTube) }
      field("Soundcloud") { checkbox("", songAvailabilityData.onSoundcloud) }
    }
  }

  override fun onSave() {
    isComplete = songAvailabilityData.commit()
  }
}

class SongVisualizationData : Fragment("Visualization") {
  private val songVisualizationData: SongPropertyVisualizationDataModel by inject()

  //----------------------------------v
  //------- Visualization Data -------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Visualizer") { checkbox("", songVisualizationData.hasVisualizer) }
      field("AMV") { checkbox("", songVisualizationData.hasAnimeMV) }
      field("Music Video") { checkbox("", songVisualizationData.hasRealMV) }
    }
  }

  override fun onSave() {
    isComplete = songVisualizationData.commit()
  }
}

@InternalAPI
@ExperimentalSerializationApi
class SongAlbumEPData : Fragment("Album/EP") {
  private val songAlbumEPData: SongPropertyAlbumEPDataModel by inject()
  private val albumTypeList = FXCollections.observableArrayList(getAlbumTypeList())!!
  private val discographyController: DiscographyController by inject()

  //----------------------------------v
  //---------- Album/EP Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset("Album") {
      field("Part of Album") { checkbox("", songAlbumEPData.inAlbum) }
      field("Name")
      {
        hbox {
          textfield(songAlbumEPData.nameAlbum) {
            contextmenu {
              item("Load album").action {
                val album = discographyController.selectAndReturnEntry()
                songAlbumEPData.inAlbum.value = true
                songAlbumEPData.nameAlbum.value = album.name
                songAlbumEPData.typeAlbum.value = album.type
                songAlbumEPData.albumUID.value = album.uID
              }
            }
          }
          label(songAlbumEPData.albumUID) { paddingHorizontal = 20 }
        }
      }
      field("Type") { combobox(songAlbumEPData.typeAlbum, albumTypeList) }
    }
  }

  override fun onSave() {
    isComplete = songAlbumEPData.commit()
  }
}

class SongStatisticsData : Fragment("Statistics") {
  private val songStatisticsData: SongPropertyStatisticsDataModel by inject()

  //----------------------------------v
  //-------- Statistics Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset {
      field("Spotify") { textfield(songStatisticsData.playsSpotify) }
      field("YouTube") { textfield(songStatisticsData.playsYouTube) }
      field("Soundcloud") { textfield(songStatisticsData.playsSoundCloud) }
    }
  }

  override fun onSave() {
    isComplete = songStatisticsData.commit()
  }
}

@InternalAPI
@ExperimentalSerializationApi
class SongCollaborationData : Fragment("Collaboration") {
  private val contactController: ContactController by inject()
  private val songCollaborationData: SongPropertyCollaborationDataModel by inject()

  //----------------------------------v
  //---------- Feature Data ----------|
  //----------------------------------^
  //----------------------------------v
  //---------- Collab Data -----------|
  //----------------------------------^
  override val root = form {
    hbox(20) {
      fieldset("Vocalist Feature") {
        field("Feat Vox 1") {
          hbox {
            textfield(songCollaborationData.coVocalist1) {
              contextmenu {
                item("Show contact").action {
                  if (songCollaborationData.coVocalist1UID.value != -1) {
                    contactController.showEntry(songCollaborationData.coVocalist1UID.value)
                  }
                  songCollaborationData.coVocalist1.value =
                    contactController.getContactName(
                      songCollaborationData.coVocalist1UID.value,
                      songCollaborationData.coVocalist1.value
                    )
                }
                item("Load contact").action {
                  val contact = contactController.selectAndLoadContact()
                  songCollaborationData.coVocalist1UID.value = contact.uID
                  songCollaborationData.coVocalist1.value = contact.name
                }
              }
            }
            label(songCollaborationData.coVocalist1UID) { paddingHorizontal = 20 }
          }
        }
        field("Feat Vox 2") {
          hbox {
            textfield(songCollaborationData.coVocalist2) {
              contextmenu {
                item("Show contact").action {
                  if (songCollaborationData.coVocalist2UID.value != -1) contactController.showEntry(
                    songCollaborationData.coVocalist2UID.value
                  )
                  songCollaborationData.coVocalist2.value =
                    contactController.getContactName(
                      songCollaborationData.coVocalist2UID.value,
                      songCollaborationData.coVocalist2.value
                    )
                }
                item("Load contact").action {
                  val contact = contactController.selectAndLoadContact()
                  songCollaborationData.coVocalist2UID.value = contact.uID
                  songCollaborationData.coVocalist2.value = contact.name
                }
              }
            }
            label(songCollaborationData.coVocalist2UID) { paddingHorizontal = 20 }
          }
        }
      }
      fieldset("Producer Collaboration") {
        field("Coproducer 1") {
          hbox {
            textfield(songCollaborationData.coProducer1) {
              contextmenu {
                item("Show contact").action {
                  if (songCollaborationData.coProducer1UID.value != -1) contactController.showEntry(
                    songCollaborationData.coProducer1UID.value
                  )
                  songCollaborationData.coProducer1.value =
                    contactController.getContactName(
                      songCollaborationData.coProducer1UID.value,
                      songCollaborationData.coProducer1.value
                    )
                }
                item("Load contact").action {
                  val contact = contactController.selectAndLoadContact()
                  songCollaborationData.coProducer1UID.value = contact.uID
                  songCollaborationData.coProducer1.value = contact.name
                }
              }
            }
            label(songCollaborationData.coProducer1UID) { paddingHorizontal = 20 }
          }
        }
        field("Coproducer 2") {
          hbox {
            textfield(songCollaborationData.coProducer2) {
              contextmenu {
                item("Show contact").action {
                  if (songCollaborationData.coProducer2UID.value != -1) contactController.showEntry(
                    songCollaborationData.coProducer2UID.value
                  )
                  songCollaborationData.coProducer2.value =
                    contactController.getContactName(
                      songCollaborationData.coProducer2UID.value,
                      songCollaborationData.coProducer2.value
                    )
                }
                item("Load contact").action {
                  val contact = contactController.selectAndLoadContact()
                  songCollaborationData.coProducer2UID.value = contact.uID
                  songCollaborationData.coProducer2.value = contact.name
                }
              }
            }
            label(songCollaborationData.coProducer2UID) { paddingHorizontal = 20 }
          }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = songCollaborationData.commit()
  }
}

class SongCopyrightData : Fragment("Copyright") {
  private val songCopyrightData: SongPropertyCopyrightDataModel by inject()

  //----------------------------------v
  //--------- Copyright Data ---------|
  //----------------------------------^
  override val root = form {
    fieldset("Copyright") {
      field("Protected") { checkbox("", songCopyrightData.isProtected) }
    }
    fieldset("Contains...") {
      field("Copyrighted Material") { checkbox("", songCopyrightData.containsCRMaterial) }
      field("Explicit Lyrics") { checkbox("", songCopyrightData.containsExplicitLyrics) }
    }
  }

  override fun onSave() {
    isComplete = songCopyrightData.commit()
  }
}

class SongMiscData : Fragment("Misc") {
  private val songMiscData: SongPropertyMiscDataModel by inject()

  //----------------------------------v
  //----------- Misc Data ------------|
  //----------------------------------^
  override val root = form {
    vbox {
      hbox(20) {
        fieldset("Inspired by...") {
          field("Artist") { textfield(songMiscData.inspiredByArtist) }
          field("Song") { textfield(songMiscData.inspiredBySong) }
        }
        fieldset("Soft-/Hardware") {
          field("DAW") { textfield(songMiscData.dawUsed) }
          field("Microphone") { textfield(songMiscData.micUsed) }
        }
        //----------------------------------v
        //------------ API Data ------------|
        //----------------------------------^
        fieldset("API") {
          field("Spotify ID") { textfield(songMiscData.spotifyID) }
        }
      }
      fieldset("Comment") {
        field {
          textfield(songMiscData.comment) {
            style {
              useMaxSize = true
            }
          }
        }
      }
    }
  }

  override fun onSave() {
    isComplete = songMiscData.commit()
  }
}
