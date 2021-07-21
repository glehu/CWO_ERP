package modules.m1.gui

import javafx.collections.FXCollections
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.getGenreList
import modules.m1.logic.M1Controller
import modules.m1.misc.SongModel
import tornadofx.*

//This Wizard is used to create new songs
@ExperimentalSerializationApi
class SongConfiguratorWizard : Wizard("Add new song")
{
    val song: SongModel by inject()

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
    val song: SongModel by inject()

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
    private val song: SongModel by inject()
    private val m1controller: M1Controller by inject()

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    private val genreList = FXCollections.observableArrayList(getGenreList())!!

    override val root = form {
        fieldset {
            field("Name") { textfield(song.name).required() }
            field("Vocalist") {
                textfield(song.vocalist).required()
                label(song.vocalistUID)
                button("<") {
                    tooltip("Load an address")
                    action {
                        val contact = m1controller.selectContact()
                        song.vocalistUID.value = contact.uID
                        song.vocalist.value = contact.name
                    }
                }
            }
            field("Producer") {
                textfield(song.producer).required()
                label(song.producerUID)
                button("<") {
                    tooltip("Load an address")
                    action {
                        val contact = m1controller.selectContact()
                        song.producerUID.value = contact.uID
                        song.producer.value = contact.name
                    }
                }
            }
            field("Mixing") { textfield(song.mixing) }
            field("Mastering") { textfield(song.mastering) }
            fieldset("Genre") { combobox(song.genre, genreList) }
            field("Subgenre") { textfield(song.subgenre) }
            field("Length") { textfield(song.songLength) }
            field("Vibe") { textfield(song.vibe) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.name,
            song.vocalist,
            song.vocalistUID,
            song.producer,
            song.producerUID,
            song.mixing,
            song.mixingUID,
            song.mastering,
            song.masteringUID,
            song.genre,
            song.subgenre,
            song.songLength,
            song.vibe
        )
    }
}

class NewSongCompletionStateData : Fragment("Completion State")
{
    private val song: SongModel by inject()

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
            fieldset("Song state") { combobox(song.songState, songStateList) }
            fieldset("Instrumental state") { combobox(song.instruState, instruStateList) }
            fieldset("Lyrics state") { combobox(song.lyricsState, lyricsStateList) }
            fieldset("Vocals state") { combobox(song.vocalsState, vocalsStateList) }
            fieldset("Mixing state") { combobox(song.mixingState, mixingStateList) }
            fieldset("Mastering state") { combobox(song.masteringState, masteringStateList) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.songState,
            song.instruState,
            song.lyricsState,
            song.vocalsState,
            song.mixingState,
            song.masteringState
        )
    }
}

class NewSongPromotionData : Fragment("Promotion")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //--------- Promotion Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Promoted") { checkbox("", song.isPromoted) }
            field("Distributed") { checkbox("", song.distributed) }
            field("Exclusive Release") { checkbox("", song.isExclusiveRelease) }
            field("Exclusive Channel") { textfield(song.exclusiveChannel) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.isPromoted,
            song.distributed,
            song.isExclusiveRelease,
            song.exclusiveChannel
        )
    }
}

class NewSongFinancialData : Fragment("Finances")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //--------- Financial Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Money Spent") { field { textfield(song.moneySpent) } }
        fieldset("Money Earned") {
            field("Streams") { textfield(song.moneyGainedStreams) }
            field("Sponsoring") { textfield(song.moneyGainedSponsor) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.moneySpent,
            song.moneyGainedStreams,
            song.moneyGainedSponsor
        )
    }
}

class NewSongAvailabilityData : Fragment("Availability")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //------- Availability Data --------|
    //----------------------------------^
    override val root = form {
        fieldset("Release") {
            field("Public") { checkbox("", song.isPublic) }
            field("Date") { datepicker(song.releaseDate) }
        }
        fieldset("Platforms") {
            field("Spotify") { checkbox("", song.onSpotify) }
            field("YouTube") { checkbox("", song.onYouTube) }
            field("Soundcloud") { checkbox("", song.onSoundCloud) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.isPublic,
            song.releaseDate,
            song.onSpotify,
            song.onYouTube,
            song.onSoundCloud
        )
    }
}

class NewSongVisualizationData : Fragment("Visualization")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //------- Visualization Data -------|
    //----------------------------------^
    override val root = form {
        fieldset {
            field("Visualizer") { checkbox("", song.hasVisualizer) }
            field("AMV") { checkbox("", song.hasAnimeMV) }
            field("Music Video") { checkbox("", song.hasRealMV) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.hasVisualizer,
            song.hasAnimeMV,
            song.hasRealMV
        )
    }
}

class NewSongAlbumEPData : Fragment("Album/EP")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //---------- Album/EP Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("EP") {
            field("Part of EP") { checkbox("", song.inEP) }
            field("Name") { textfield(song.nameEP) }
        }
        fieldset("Album") {
            field("Part of Album") { checkbox("", song.inAlbum) }
            field("Name") { textfield(song.nameAlbum) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.inEP,
            song.nameEP,
            song.inAlbum,
            song.nameAlbum
        )
    }
}

class NewSongStatisticsData : Fragment("Statistics")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Spotify") {
            field("Streams") { textfield(song.playsSpotify) }
        }
        fieldset("YouTube") {
            field("Views") { textfield(song.playsYouTube) }
        }
        fieldset("Soundcloud") {
            field("Streams") { textfield(song.playsSoundCloud) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.playsSpotify,
            song.playsYouTube,
            song.playsSoundCloud
        )
    }
}

class NewSongCollaborationData : Fragment("Collaboration")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //---------- Feature Data ----------|
    //----------------------------------^
    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    override val root = form {
        fieldset("Vocalist Feature") {
            field("Feat Vox 1") { textfield(song.coVocalist1) }
            field("Feat Vox 2") { textfield(song.coVocalist2) }
        }
        fieldset("Producer Collaboration") {
            field("Coproducer 1") { textfield(song.coProducer1) }
            field("Coproducer 2") { textfield(song.coProducer2) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.coVocalist1,
            song.coVocalist2,
            song.coProducer1,
            song.coProducer2
        )
    }
}

class NewSongCopyrightData : Fragment("Copyright")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    override val root = form {
        fieldset("Copyright") {
            field("Protected") { checkbox("", song.isProtected) }
        }
        fieldset("Contains...") {
            field("Copyrighted Material") { checkbox("", song.containsCRMaterial) }
            field("Explicit Lyrics") { checkbox("", song.containsExplicitLyrics) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.isProtected,
            song.containsCRMaterial,
            song.containsExplicitLyrics
        )
    }
}

class NewSongMiscData : Fragment("Misc")
{
    private val song: SongModel by inject()

    //----------------------------------v
    //----------- Misc Data ------------|
    //----------------------------------^
    override val root = form {
        fieldset("Inspired by...") {
            field("Artist") { textfield(song.inspiredByArtist) }
            field("Song") { textfield(song.inspiredBySong) }
        }
        fieldset("Soft-/Hardware") {
            field("DAW") { textfield(song.dawUsed) }
            field("Microphone") { textfield(song.micUsed) }
        }
        fieldset("Comment") {
            field { textfield(song.comment) }
        }
    }

    override fun onSave()
    {
        isComplete = song.commit(
            song.inspiredByArtist,
            song.inspiredBySong,
            song.dawUsed,
            song.micUsed,
            song.comment
        )
    }
}