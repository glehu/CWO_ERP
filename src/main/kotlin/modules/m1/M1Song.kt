package modules.m1

import db.CwODB
import kotlinx.serialization.Serializable
import modules.Entry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class Song(var uID: Int, var name: String): Entry
{
    /*  M1SONGS DB Version 0.1.5-PreAlpha

            Changelog:
                12.10.2020 V0.1.0-PreAlpha
                    Module created
                13.10.2020 V0.1.1-PreAlpha
                    Added more information
                14.10.2020 V0.1.2-PreAlpha
                    Added template
                    Added init() function including Regex
                    Added more information
                15.10.2020 V0.1.3-PreAlpha
                    Moved some information to auto generated data
                    More auto generated data
                01.12.2020 V0.1.4-PreAlpha
                    Added byteSize
                17.04.2021 V0.1.5-PreAlpha
                    Added completion state
    */
    //*************************************************
    //********************** User Input Data **********
    //*************************************************

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    var vocalist: String = "?"
    var producer: String = "?"
    var mixing: String = "?"
    var mastering: String = "?"
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
    var coVocalist2: String = "?"

    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    var coProducer1: String = "?"
    var coProducer2: String = "?"

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

    //Database relevant information
    var byteSize: Long = 0L

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

    fun initialize()
    {
        if (uID == -1)
        {
            val indexer = CwODB()
            uID = indexer.getUniqueID("M1")
        }

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
        } else
        {
            songLengthHours = 0.0; songLengthMinutes = 0.0; songLengthSeconds = 0.0
        }
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
        if (onSpotify)
        {
            nTmp1 += 1; totalPlays += playsSpotify
        }
        if (onYouTube)
        {
            nTmp1 += 1; totalPlays += playsYouTube
        }
        if (onSoundCloud)
        {
            nTmp1 += 1; totalPlays += playsSoundCloud
        }
        averagePlays = totalPlays.toDouble() / nTmp1
        //----------------- Single/EP/Album
        isSingle = !(inEP || inAlbum)
        //----------------- Collaboration
        hasFeature = (coVocalist1.isNotEmpty()) && (coVocalist2.isNotEmpty())
        isCollab = (coProducer1.isNotEmpty()) && (coProducer2.isNotEmpty())
    }
}

fun getSongPropertyFromSong(song: Song): SongProperty
{
    val songProperty = SongProperty()
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    songProperty.uniqueID = song.uID
    songProperty.name = song.name
    songProperty.vocalist = song.vocalist
    songProperty.producer = song.producer
    songProperty.mixing = song.mixing
    songProperty.mastering = song.mastering
    songProperty.genre = song.genre
    songProperty.subgenre = song.subgenre
    songProperty.songLength = song.songLength
    songProperty.vibe = song.vibe
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    songProperty.songState = song.songState
    songProperty.instruState = song.instruState
    songProperty.lyricsState = song.lyricsState
    songProperty.vocalsState = song.vocalsState
    songProperty.mixingState = song.mixingState
    songProperty.masteringState = song.masteringState
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    songProperty.isPromoted = song.isPromoted
    songProperty.distributed = song.distributed
    songProperty.isExclusiveRelease = song.isExclusiveRelease
    songProperty.exclusiveChannel = song.exclusiveChannel
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    songProperty.moneySpent = song.moneySpent
    songProperty.moneyGainedStreams = song.moneyGainedStreams
    songProperty.moneyGainedSponsor = song.moneyGainedSponsor
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    songProperty.isPublic = song.isPublic
    songProperty.releaseDate = LocalDate.parse(song.releaseDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    songProperty.onSpotify = song.onSpotify
    songProperty.onYouTube = song.onYouTube
    songProperty.onSoundCloud = song.onSoundCloud
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    songProperty.hasVisualizer = song.hasVisualizer
    songProperty.hasAnimeMV = song.hasAnimeMV
    songProperty.hasRealMV = song.hasRealMV
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    songProperty.inEP = song.inEP
    songProperty.inAlbum = song.inAlbum
    songProperty.nameEP = song.nameEP
    songProperty.nameAlbum = song.nameAlbum
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    songProperty.playsSpotify = song.playsSpotify
    songProperty.playsYouTube = song.playsYouTube
    songProperty.playsSoundCloud = song.playsSoundCloud
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    songProperty.coVocalist1 = song.coVocalist1
    songProperty.coVocalist2 = song.coVocalist2
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    songProperty.coProducer1 = song.coProducer1
    songProperty.coProducer2 = song.coProducer2
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    songProperty.isProtected = song.isProtected
    songProperty.containsCRMaterial = song.containsCRMaterial
    songProperty.containsExplicitLyrics = song.containsExplicitLyrics
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    songProperty.inspiredByArtist = song.inspiredByArtist
    songProperty.inspiredBySong = song.inspiredBySong
    songProperty.dawUsed = song.dawUsed
    songProperty.micUsed = song.micUsed
    songProperty.comment = song.comment
    songProperty.byteSize = song.byteSize
    return songProperty
}

fun getSongFromProperty(songProperty: SongProperty): Song
{
    val song = Song(-1, songProperty.name)
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    song.uID = songProperty.uniqueID
    song.vocalist = songProperty.vocalist
    song.producer = songProperty.producer
    song.mixing = songProperty.mixing
    song.mastering = songProperty.mastering
    song.genre = songProperty.genre
    song.subgenre = songProperty.subgenre
    song.songLength = songProperty.songLength
    song.vibe = songProperty.vibe
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    song.songState = songProperty.songState
    song.instruState = songProperty.instruState
    song.lyricsState = songProperty.lyricsState
    song.vocalsState = songProperty.vocalsState
    song.mixingState = songProperty.mixingState
    song.masteringState = songProperty.masteringState
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    song.isPromoted = songProperty.isPromoted
    song.distributed = songProperty.distributed
    song.isExclusiveRelease = songProperty.isExclusiveRelease
    song.exclusiveChannel = songProperty.exclusiveChannel
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    song.moneySpent = songProperty.moneySpent
    song.moneyGainedStreams = songProperty.moneyGainedStreams
    song.moneyGainedSponsor = songProperty.moneyGainedSponsor
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    song.isPublic = songProperty.isPublic
    song.releaseDate = songProperty.releaseDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    song.onSpotify = songProperty.onSpotify
    song.onYouTube = songProperty.onYouTube
    song.onSoundCloud = songProperty.onSoundCloud
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    song.hasVisualizer = songProperty.hasVisualizer
    song.hasAnimeMV = songProperty.hasAnimeMV
    song.hasRealMV = songProperty.hasRealMV
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    song.inEP = songProperty.inEP
    song.inAlbum = songProperty.inAlbum
    song.nameEP = songProperty.nameEP
    song.nameAlbum = songProperty.nameAlbum
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    song.playsSpotify = songProperty.playsSpotify
    song.playsYouTube = songProperty.playsYouTube
    song.playsSoundCloud = songProperty.playsSoundCloud
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    song.coVocalist1 = songProperty.coVocalist1
    song.coVocalist2 = songProperty.coVocalist2
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    song.coProducer1 = songProperty.coProducer1
    song.coProducer2 = songProperty.coProducer2
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    song.isProtected = songProperty.isProtected
    song.containsCRMaterial = songProperty.containsCRMaterial
    song.containsExplicitLyrics = songProperty.containsExplicitLyrics
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    song.inspiredByArtist = songProperty.inspiredByArtist
    song.inspiredBySong = songProperty.inspiredBySong
    song.dawUsed = songProperty.dawUsed
    song.micUsed = songProperty.micUsed
    song.comment = songProperty.comment
    song.byteSize = songProperty.byteSize
    return song
}

//Template
/*
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    song.vocalist                   = "?"
    song.producer                   = "?"
    song.mixing                     = "?"
    song.mastering                  = "?"
    song.genre                      = "?"
    song.subgenre                   = "?"
    song.songLength                 = "??:??"
    song.vibe                       = "?"
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    song.songState                  = "?"
    song.instruState                = "?"
    song.lyricsState                = "?"
    song.vocalsState                = "?"
    song.mixingState                = "?"
    song.masteringState             = "?"
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    song.isPromoted                 = false
    song.distributed                = false
    song.isExclusiveRelease         = false
    song.exclusiveChannel           = "?"
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    song.moneySpent                 = 0.0
    song.moneyGainedStreams         = 0.0
    song.moneyGainedSponsor         = 0.0
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    song.isPublic                   = false
    song.releaseDate                = "??.??.????"
    song.onSpotify                  = false
    song.onYouTube                  = false
    song.onSoundCloud               = false
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    song.hasVisualizer              = false
    song.hasAnimeMV                 = false
    song.hasRealMV                  = false
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    song.inEP                       = false
    song.inAlbum                    = false
    song.nameEP                     = "?"
    song.nameAlbum                  = "?"
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    song.playsSpotify               = 0
    song.playsYouTube               = 0
    song.playsSoundCloud            = 0
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    song.coVocalist1                = "?"
    song.coVocalist2                = "?"
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    song.coProducer1                = "?"
    song.coProducer2                = "?"
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    song.isProtected                = false
    song.containsCRMaterial         = false
    song.containsExplicitLyrics     = false
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    song.inspiredByArtist           = "?"
    song.inspiredBySong             = "?"
    song.dawUsed                    = "?"
    song.micUsed                    = "?"
    song.comment                    = "?"
    song.byteSize                   = 0L
 */