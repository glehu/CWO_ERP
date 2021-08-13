package modules.m1.gui

import db.CwODB
import javafx.collections.FXCollections
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.getGenreList
import modules.m1.misc.SongModelP1
import modules.m1.misc.SongModelP2
import modules.m2.logic.M2Controller
import tornadofx.*

//This Wizard is used to create new songs
@ExperimentalSerializationApi
class SongConfiguratorWizard : Wizard("Add new song")
{
    val songP1: SongModelP1 by inject()
    val songP2: SongModelP2 by inject()

    init
    {
        enableStepLinks = true
        add(NewSongMainData::class)
        add(NewSongCompletionStateData::class)
        add(NewSongPromotionData::class)
        add(NewSongFinancialData::class)
        add(NewSongAvailabilityData::class)
        add(NewSongVisualizationData::class)
        add(NewSongAlbumEPData::class)
        add(NewSongStatisticsData::class)
        add(NewSongCollaborationData::class)
        add(NewSongCopyrightData::class)
        add(NewSongMiscData::class)
    }
}

//This Wizard is used to view and/or edit songs
@ExperimentalSerializationApi
class SongViewerWizard : Wizard("View a song")
{
    val songP1: SongModelP1 by inject()
    val songP2: SongModelP2 by inject()

    init
    {
        enableStepLinks = true
        add(NewSongMainData::class)
        add(NewSongCompletionStateData::class)
        add(NewSongPromotionData::class)
        add(NewSongFinancialData::class)
        add(NewSongAvailabilityData::class)
        add(NewSongVisualizationData::class)
        add(NewSongAlbumEPData::class)
        add(NewSongStatisticsData::class)
        add(NewSongCollaborationData::class)
        add(NewSongCopyrightData::class)
        add(NewSongMiscData::class)
    }
}

@ExperimentalSerializationApi
class NewSongMainData : Fragment("Main")
{
    val db: CwODB by inject()
    private val songP1: SongModelP1 by inject()
    private val m2controller: M2Controller by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    private val genreList = FXCollections.observableArrayList(getGenreList())!!

    override val root = form {
        fieldset {
            field("Name") { textfield(songP1.name).required() }
            field("Vocalist") {
                hbox {
                    textfield(songP1.vocalist) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP1.vocalistUID.value != -1) m2controller.showContact(songP1.vocalistUID.value)
                                songP1.vocalist.value =
                                    m2controller.getContactName(songP1.vocalistUID.value, songP1.vocalist.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP1.vocalistUID.value = contact.uID
                                songP1.vocalist.value = contact.name
                            }
                        }
                    }.required()
                    label(songP1.vocalistUID) { paddingHorizontal = 20 }
                }
            }
            field("Producer") {
                hbox {
                    textfield(songP1.producer) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP1.producerUID.value != -1) m2controller.showContact(songP1.producerUID.value)
                                songP1.producer.value =
                                    m2controller.getContactName(songP1.producerUID.value, songP1.producer.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP1.producerUID.value = contact.uID
                                songP1.producer.value = contact.name
                            }
                        }
                    }.required()
                    label(songP1.producerUID) { paddingHorizontal = 20 }
                }
            }
            field("Mixing") {
                hbox {
                    textfield(songP1.mixing) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP1.mixingUID.value != -1) m2controller.showContact(songP1.mixingUID.value)
                                songP1.mixing.value =
                                    m2controller.getContactName(songP1.mixingUID.value, songP1.mixing.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP1.mixingUID.value = contact.uID
                                songP1.mixing.value = contact.name
                            }
                        }
                    }
                    label(songP1.mixingUID) { paddingHorizontal = 20 }
                }
            }
            field("Mastering") {
                hbox {
                    textfield(songP1.mastering) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP1.masteringUID.value != -1) m2controller.showContact(songP1.masteringUID.value)
                                songP1.mastering.value =
                                    m2controller.getContactName(songP1.masteringUID.value, songP1.mastering.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP1.masteringUID.value = contact.uID
                                songP1.mastering.value = contact.name
                            }
                        }
                    }
                    label(songP1.masteringUID) { paddingHorizontal = 20 }
                }
            }
            fieldset("Genre") { combobox(songP1.genre, genreList) }
            field("Subgenre") { textfield(songP1.subgenre) }
            field("Length") { textfield(songP1.songLength) }
            field("Vibe") { textfield(songP1.vibe) }
        }
    }

    override fun onSave()
    {
        isComplete = songP1.commit(
            songP1.name,
            songP1.vocalist,
            songP1.vocalistUID,
            songP1.producer,
            songP1.producerUID,
            songP1.mixing,
            songP1.mixingUID,
            songP1.mastering,
            songP1.masteringUID,
            songP1.genre,
            songP1.subgenre,
            songP1.songLength,
            songP1.vibe
        )
    }
}

class NewSongCompletionStateData : Fragment("Completion State")
{
    private val songP1: SongModelP1 by inject()

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
            fieldset("Song state") { combobox(songP1.songState, songStateList) }
            fieldset("Instrumental state") { combobox(songP1.instruState, instruStateList) }
            fieldset("Lyrics state") { combobox(songP1.lyricsState, lyricsStateList) }
            fieldset("Vocals state") { combobox(songP1.vocalsState, vocalsStateList) }
            fieldset("Mixing state") { combobox(songP1.mixingState, mixingStateList) }
            fieldset("Mastering state") { combobox(songP1.masteringState, masteringStateList) }
        }
    }

    override fun onSave()
    {
        isComplete = songP1.commit(
            songP1.songState,
            songP1.instruState,
            songP1.lyricsState,
            songP1.vocalsState,
            songP1.mixingState,
            songP1.masteringState
        )
    }
}

class NewSongPromotionData : Fragment("Promotion")
{
    private val songP1: SongModelP1 by inject()

    //----------------------------------v
    //--------- Promotion Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Promoted") { checkbox("", songP1.isPromoted) }
            field("Distributed") { checkbox("", songP1.distributed) }
            field("Exclusive Release") { checkbox("", songP1.isExclusiveRelease) }
            field("Exclusive Channel") { textfield(songP1.exclusiveChannel) }
        }
    }

    override fun onSave()
    {
        isComplete = songP1.commit(
            songP1.isPromoted,
            songP1.distributed,
            songP1.isExclusiveRelease,
            songP1.exclusiveChannel
        )
    }
}

class NewSongFinancialData : Fragment("Finances")
{
    private val songP1: SongModelP1 by inject()

    //----------------------------------v
    //--------- Financial Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Money Spent") { field { textfield(songP1.moneySpent) } }
        fieldset("Money Earned") {
            field("Streams") { textfield(songP1.moneyGainedStreams) }
            field("Sponsoring") { textfield(songP1.moneyGainedSponsor) }
        }
    }

    override fun onSave()
    {
        isComplete = songP1.commit(
            songP1.moneySpent,
            songP1.moneyGainedStreams,
            songP1.moneyGainedSponsor
        )
    }
}

class NewSongAvailabilityData : Fragment("Availability")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //------- Availability Data --------|
    //----------------------------------^
    override val root = form {
        fieldset("Release") {
            field("Public") { checkbox("", songP2.isPublic) }
            field("Date") { datepicker(songP2.releaseDate) }
        }
        fieldset("Platforms") {
            field("Spotify") { checkbox("", songP2.onSpotify) }
            field("YouTube") { checkbox("", songP2.onYouTube) }
            field("Soundcloud") { checkbox("", songP2.onSoundCloud) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.isPublic,
            songP2.releaseDate,
            songP2.onSpotify,
            songP2.onYouTube,
            songP2.onSoundCloud
        )
    }
}

class NewSongVisualizationData : Fragment("Visualization")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //------- Visualization Data -------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Visualizer") { checkbox("", songP2.hasVisualizer) }
            field("AMV") { checkbox("", songP2.hasAnimeMV) }
            field("Music Video") { checkbox("", songP2.hasRealMV) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.hasVisualizer,
            songP2.hasAnimeMV,
            songP2.hasRealMV
        )
    }
}

class NewSongAlbumEPData : Fragment("Album/EP")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //---------- Album/EP Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("EP") {
            field("Part of EP") { checkbox("", songP2.inEP) }
            field("Name") { textfield(songP2.nameEP) }
        }
        fieldset("Album") {
            field("Part of Album") { checkbox("", songP2.inAlbum) }
            field("Name") { textfield(songP2.nameAlbum) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.inEP,
            songP2.nameEP,
            songP2.inAlbum,
            songP2.nameAlbum
        )
    }
}

class NewSongStatisticsData : Fragment("Statistics")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Spotify") {
            field("Streams") { textfield(songP2.playsSpotify) }
        }
        fieldset("YouTube") {
            field("Views") { textfield(songP2.playsYouTube) }
        }
        fieldset("Soundcloud") {
            field("Streams") { textfield(songP2.playsSoundCloud) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.playsSpotify,
            songP2.playsYouTube,
            songP2.playsSoundCloud
        )
    }
}

@ExperimentalSerializationApi
class NewSongCollaborationData : Fragment("Collaboration")
{
    private val db: CwODB by inject()
    private val songP2: SongModelP2 by inject()
    private val m2controller: M2Controller by inject()

    //----------------------------------v
    //---------- Feature Data ----------|
    //----------------------------------^
    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    override val root = form {
        fieldset("Vocalist Feature") {
            field("Feat Vox 1") {
                hbox {
                    textfield(songP2.coVocalist1) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP2.coVocalist1UID.value != -1) m2controller.showContact(songP2.coVocalist1UID.value)
                                songP2.coVocalist1.value =
                                    m2controller.getContactName(songP2.coVocalist1UID.value, songP2.coVocalist1.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP2.coVocalist1UID.value = contact.uID
                                songP2.coVocalist1.value = contact.name
                            }
                        }
                    }
                    label(songP2.coVocalist1UID) { paddingHorizontal = 20 }
                }
            }
            field("Feat Vox 2") {
                hbox {
                    textfield(songP2.coVocalist2) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP2.coVocalist2UID.value != -1) m2controller.showContact(songP2.coVocalist2UID.value)
                                songP2.coVocalist2.value =
                                    m2controller.getContactName(songP2.coVocalist2UID.value, songP2.coVocalist2.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP2.coVocalist2UID.value = contact.uID
                                songP2.coVocalist2.value = contact.name
                            }
                        }
                    }
                    label(songP2.coVocalist2UID) { paddingHorizontal = 20 }
                }
            }
        }
        fieldset("Producer Collaboration") {
            field("Coproducer 1") {
                hbox {
                    textfield(songP2.coProducer1) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP2.coProducer1UID.value != -1) m2controller.showContact(songP2.coProducer1UID.value)
                                songP2.coProducer1.value =
                                    m2controller.getContactName(songP2.coProducer1UID.value, songP2.coProducer1.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP2.coProducer1UID.value = contact.uID
                                songP2.coProducer1.value = contact.name
                            }
                        }
                    }
                    label(songP2.coProducer1UID) { paddingHorizontal = 20 }
                }
            }
            field("Coproducer 2") {
                hbox {
                    textfield(songP2.coProducer2) {
                        contextmenu {
                            item("Show contact").action {
                                if (songP2.coProducer2UID.value != -1) m2controller.showContact(songP2.coProducer2UID.value)
                                songP2.coProducer2.value =
                                    m2controller.getContactName(songP2.coProducer2UID.value, songP2.coProducer2.value)
                            }
                            item("Load contact").action {
                                val contact = m2controller.selectAndReturnContact()
                                songP2.coProducer2UID.value = contact.uID
                                songP2.coProducer2.value = contact.name
                            }
                        }
                    }
                    label(songP2.coProducer2UID) { paddingHorizontal = 20 }
                }
            }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.coVocalist1,
            songP2.coVocalist1UID,
            songP2.coVocalist2,
            songP2.coVocalist2UID,
            songP2.coProducer1,
            songP2.coProducer1UID,
            songP2.coProducer2,
            songP2.coProducer2UID
        )
    }
}

class NewSongCopyrightData : Fragment("Copyright")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Copyright") {
            field("Protected") { checkbox("", songP2.isProtected) }
        }
        fieldset("Contains...") {
            field("Copyrighted Material") { checkbox("", songP2.containsCRMaterial) }
            field("Explicit Lyrics") { checkbox("", songP2.containsExplicitLyrics) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.isProtected,
            songP2.containsCRMaterial,
            songP2.containsExplicitLyrics
        )
    }
}

class NewSongMiscData : Fragment("Misc")
{
    private val songP2: SongModelP2 by inject()

    //----------------------------------v
    //----------- Misc Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset("Inspired by...") {
            field("Artist") { textfield(songP2.inspiredByArtist) }
            field("Song") { textfield(songP2.inspiredBySong) }
        }
        fieldset("Soft-/Hardware") {
            field("DAW") { textfield(songP2.dawUsed) }
            field("Microphone") { textfield(songP2.micUsed) }
        }
        fieldset("Comment") {
            field { textfield(songP2.comment) }
        }
    }

    override fun onSave()
    {
        isComplete = songP2.commit(
            songP2.inspiredByArtist,
            songP2.inspiredBySong,
            songP2.dawUsed,
            songP2.micUsed,
            songP2.comment
        )
    }
}