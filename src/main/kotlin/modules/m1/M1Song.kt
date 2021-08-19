package modules.m1

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import interfaces.IEntry
import modules.mx.m1GlobalIndex

@Serializable
data class Song(override var uID: Int, var name: String) : IEntry
{
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
    var releaseDate: String = "??.??.????"
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
    var inEP: Boolean = false
    var inAlbum: Boolean = false
    var nameEP: String = "?"
    var nameAlbum: String = "?"

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

    //*************************************************
    //****************** Auto Generated Data **********
    //*************************************************

    //Time
    private var songLengthHours: Double = 0.0
    private var songLengthMinutes: Double = 0.0
    private var songLengthSeconds: Double = 0.0

    //Money
    private var moneySpentPerMinute: Double = 0.0
    private var moneyGainedTotal: Double = 0.0
    private var moneyGainedPerMinute: Double = 0.0

    //Stats
    private var totalPlays: Int = 0
    private var averagePlays: Double = 0.0

    //Single/EP/Album
    private var isSingle: Boolean = false

    //Collaboration
    private var hasFeature: Boolean = false
    private var isCollab: Boolean = false

    @ExperimentalSerializationApi
    fun initialize()
    {
        if (uID == -1) uID = m1GlobalIndex.getUID()

        val sTmp1: String
        val sTmp2: String
        //----------------- Time
        val regex = "^[0-9]?[0-9]:[0-9][0-9]$".toRegex()
        if (regex.matches(songLength))
        {
            sTmp1 = songLength.substringAfter(':')
            sTmp2 = songLength.substringBefore(':')
            val seconds: Double = ((sTmp1.toDouble()) + (sTmp2.toDouble() * 60))
            songLengthHours = ((seconds / 60) / 60)
            songLengthMinutes = (seconds / 60)
            songLengthSeconds = seconds
        } else songLengthHours = 0.0; songLengthMinutes = 0.0; songLengthSeconds = 0.0
        //----------------- Money
        moneyGainedTotal = moneyGainedStreams + moneyGainedSponsor
        if (songLengthMinutes > 0.0)
        {
            moneySpentPerMinute = moneySpent / songLengthMinutes
            moneyGainedPerMinute = moneyGainedTotal / songLengthMinutes
        }
        //----------------- Statistics
        totalPlays = 0
        var nTmp1 = 0
        if (onSpotify) nTmp1 += 1; totalPlays += playsSpotify
        if (onYouTube) nTmp1 += 1; totalPlays += playsYouTube
        if (onSoundCloud) nTmp1 += 1; totalPlays += playsSoundCloud
        averagePlays = if (nTmp1 > 0) totalPlays.toDouble() / nTmp1 else 0.0
        //----------------- Single/EP/Album
        isSingle = !(inEP || inAlbum)
        //----------------- Collaboration
        hasFeature = (coVocalist1.isNotEmpty()) && (coVocalist2.isNotEmpty())
        isCollab = (coProducer1.isNotEmpty()) && (coProducer2.isNotEmpty())
    }
}