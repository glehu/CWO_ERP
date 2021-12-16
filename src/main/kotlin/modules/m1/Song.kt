package modules.m1

import interfaces.IEntry
import io.ktor.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import modules.mx.logic.getDefaultDate
import modules.mx.discographyIndexManager

@InternalAPI
@ExperimentalSerializationApi
@Serializable
data class Song(override var uID: Int, var name: String) : IEntry {
    //*************************************************
    //********************** User Input Data **********
    //*************************************************

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    var vocalist: String = "?"
    var vocalistUID: Int = -1
    var producer: String = "?"
    var producerUID: Int = -1
    var mixing: String = "?"
    var mixingUID: Int = -1
    var mastering: String = "?"
    var masteringUID: Int = -1
    var type: String = "?"
    var genre: String = "?"
    var subgenre: String = "?"
    var songLength: String = "??:??"
    var vibe: String = "?"

    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    var songState: String = "?"
    var instruState: String = "?"
    var lyricsState: String = "?"
    var vocalsState: String = "?"
    var mixingState: String = "?"
    var masteringState: String = "?"

    //----------------------------------v
    //--------- Promotion Data ---------|
    //----------------------------------^
    var isPromoted: Boolean = false
    var distributed: Boolean = false
    var isExclusiveRelease: Boolean = false
    var exclusiveChannel: String = "?"

    //----------------------------------v
    //--------- Financial Data ---------|
    //----------------------------------^
    var moneySpent: Double = 0.0
    var moneyGainedStreams: Double = 0.0
    var moneyGainedSponsor: Double = 0.0

    //----------------------------------v
    //------- Availability Data --------|
    //----------------------------------^
    var isPublic: Boolean = false
    var releaseDate: String = getDefaultDate()
    var onSpotify: Boolean = false
    var onYouTube: Boolean = false
    var onSoundCloud: Boolean = false

    //----------------------------------v
    //------- Visualization Data -------|
    //----------------------------------^
    var hasVisualizer: Boolean = false
    var hasAnimeMV: Boolean = false
    var hasRealMV: Boolean = false

    //----------------------------------v
    //---------- Album/EP Data ---------|
    //----------------------------------^
    var inAlbum: Boolean = false
    var nameAlbum: String = "?"
    var typeAlbum: String = "?"
    var albumUID: Int = -1

    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    var playsSpotify: Int = 0
    var playsYouTube: Int = 0
    var playsSoundCloud: Int = 0

    //----------------------------------v
    //---------- Feature Data ----------|
    //----------------------------------^
    var coVocalist1: String = "?"
    var coVocalist1UID: Int = -1
    var coVocalist2: String = "?"
    var coVocalist2UID: Int = -1

    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    var coProducer1: String = "?"
    var coProducer1UID: Int = -1
    var coProducer2: String = "?"
    var coProducer2UID: Int = -1

    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    var isProtected: Boolean = false
    var containsCRMaterial: Boolean = false
    var containsExplicitLyrics: Boolean = false

    //----------------------------------v
    //----------- Misc Data ------------|
    //----------------------------------^
    var inspiredByArtist: String = "?"
    var inspiredBySong: String = "?"
    var dawUsed: String = "?"
    var micUsed: String = "?"
    var comment: String = "?"

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    var spotifyID: String = "?"

    override fun initialize() {
        if (uID == -1) uID = discographyIndexManager!!.getUID()
    }
}
