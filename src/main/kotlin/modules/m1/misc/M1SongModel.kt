package modules.m1.misc

import javafx.beans.property.*
import modules.m1.Song
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SongPropertyP1
{
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID by uIDProperty
    val nameProperty = SimpleStringProperty()
    var name: String by nameProperty

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    val vocalistProperty = SimpleStringProperty()
    val vocalistUIDProperty = SimpleIntegerProperty(-1)
    var vocalist: String by vocalistProperty
    var vocalistUID: Int by vocalistUIDProperty
    val producerProperty = SimpleStringProperty()
    val producerUIDProperty = SimpleIntegerProperty(-1)
    var producer: String by producerProperty
    var producerUID: Int by producerUIDProperty
    val mixingProperty = SimpleStringProperty("?")
    val mixingUIDProperty = SimpleIntegerProperty(-1)
    var mixing: String by mixingProperty
    var mixingUID: Int by mixingUIDProperty
    val masteringProperty = SimpleStringProperty("?")
    val masteringUIDProperty = SimpleIntegerProperty(-1)
    var mastering: String by masteringProperty
    var masteringUID: Int by masteringUIDProperty
    val genreProperty = SimpleStringProperty("?")
    var genre: String by genreProperty
    val subgenreProperty = SimpleStringProperty("?")
    var subgenre: String by subgenreProperty
    val songLengthProperty = SimpleStringProperty("??:??")
    var songLength: String by songLengthProperty
    val vibeProperty = SimpleStringProperty("?")
    var vibe: String by vibeProperty

    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    val songStateProperty = SimpleStringProperty("?")
    var songState: String by songStateProperty
    val instruStateProperty = SimpleStringProperty("?")
    var instruState: String by instruStateProperty
    val lyricsStateProperty = SimpleStringProperty("?")
    var lyricsState: String by lyricsStateProperty
    val vocalsStateProperty = SimpleStringProperty("?")
    var vocalsState: String by vocalsStateProperty
    val mixingStateProperty = SimpleStringProperty("?")
    var mixingState: String by mixingStateProperty
    val masteringStateProperty = SimpleStringProperty("?")
    var masteringState: String by masteringStateProperty

    //----------------------------------v
    //--------- Promotion Data ---------|
    //----------------------------------^
    val isPromotedProperty = SimpleBooleanProperty(false)
    var isPromoted by isPromotedProperty
    val distributedProperty = SimpleBooleanProperty(false)
    var distributed by distributedProperty
    val isExclusiveReleaseProperty = SimpleBooleanProperty(false)
    var isExclusiveRelease by isExclusiveReleaseProperty
    val exclusiveChannelProperty = SimpleStringProperty("?")
    var exclusiveChannel: String by exclusiveChannelProperty

    //----------------------------------v
    //--------- Financial Data ---------|
    //----------------------------------^
    val moneySpentProperty = SimpleDoubleProperty(0.0)
    var moneySpent by moneySpentProperty
    val moneyGainedStreamsProperty = SimpleDoubleProperty(0.0)
    var moneyGainedStreams by moneyGainedStreamsProperty
    val moneyGainedSponsorProperty = SimpleDoubleProperty(0.0)
    var moneyGainedSponsor by moneyGainedSponsorProperty
}

class SongPropertyP2
{
    //----------------------------------v
    //------- Availability Data --------|
    //----------------------------------^
    val isPublicProperty = SimpleBooleanProperty(false)
    var isPublic by isPublicProperty
    val releaseDateProperty = SimpleObjectProperty(LocalDate.now())
    var releaseDate: LocalDate by releaseDateProperty
    val onSpotifyProperty = SimpleBooleanProperty(false)
    var onSpotify by onSpotifyProperty
    val onYouTubeProperty = SimpleBooleanProperty(false)
    var onYouTube by onYouTubeProperty
    val onSoundcloudProperty = SimpleBooleanProperty(false)
    var onSoundCloud by onSoundcloudProperty

    //----------------------------------v
    //------- Visualization Data -------|
    //----------------------------------^
    val hasVisualizerProperty = SimpleBooleanProperty(false)
    var hasVisualizer by hasVisualizerProperty
    val hasAnimeMVProperty = SimpleBooleanProperty(false)
    var hasAnimeMV by hasAnimeMVProperty
    val hasRealMVProperty = SimpleBooleanProperty(false)
    var hasRealMV by hasRealMVProperty

    //----------------------------------v
    //---------- Album/EP Data ---------|
    //----------------------------------^
    val inEPProperty = SimpleBooleanProperty(false)
    var inEP by inEPProperty
    val inAlbumProperty = SimpleBooleanProperty(false)
    var inAlbum by inAlbumProperty
    val nameEPProperty = SimpleStringProperty("?")
    var nameEP: String by nameEPProperty
    val nameAlbumProperty = SimpleStringProperty("?")
    var nameAlbum: String by nameAlbumProperty

    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    val playsSpotifyProperty = SimpleIntegerProperty(0)
    var playsSpotify by playsSpotifyProperty
    val playsYouTubeProperty = SimpleIntegerProperty(0)
    var playsYouTube by playsYouTubeProperty
    val playsSoundCloudProperty = SimpleIntegerProperty(0)
    var playsSoundCloud by playsSoundCloudProperty

    //----------------------------------v
    //---------- Feature Data ----------|
    //----------------------------------^
    val coVocalist1Property = SimpleStringProperty("?")
    val coVocalist1UIDProperty = SimpleIntegerProperty(-1)
    var coVocalist1: String by coVocalist1Property
    var coVocalist1UID: Int by coVocalist1UIDProperty
    val coVocalist2Property = SimpleStringProperty("?")
    val coVocalist2UIDProperty = SimpleIntegerProperty(-1)
    var coVocalist2: String by coVocalist2Property
    var coVocalist2UID: Int by coVocalist2UIDProperty

    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    val coProducer1Property = SimpleStringProperty("?")
    val coProducer1UIDProperty = SimpleIntegerProperty(-1)
    var coProducer1: String by coProducer1Property
    var coProducer1UID: Int by coProducer1UIDProperty
    val coProducer2Property = SimpleStringProperty("?")
    val coProducer2UIDProperty = SimpleIntegerProperty(-1)
    var coProducer2: String by coProducer2Property
    var coProducer2UID: Int by coProducer2UIDProperty

    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    val isProtectedProperty = SimpleBooleanProperty(false)
    var isProtected by isProtectedProperty
    val containsCRMaterialProperty = SimpleBooleanProperty(false)
    var containsCRMaterial by containsCRMaterialProperty
    val containsExplicitLyricsProperty = SimpleBooleanProperty(false)
    var containsExplicitLyrics by containsExplicitLyricsProperty

    //----------------------------------v
    //----------- Misc Data ------------|
    //----------------------------------^
    val inspiredByArtistProperty = SimpleStringProperty("?")
    var inspiredByArtist: String by inspiredByArtistProperty
    val inspiredBySongProperty = SimpleStringProperty("?")
    var inspiredBySong: String by inspiredBySongProperty
    val dawUsedProperty = SimpleStringProperty("?")
    var dawUsed: String by dawUsedProperty
    val micUsedProperty = SimpleStringProperty("?")
    var micUsed: String by micUsedProperty
    val commentProperty = SimpleStringProperty("?")
    var comment: String by commentProperty
}

class SongModelP1 : ItemViewModel<SongPropertyP1>(SongPropertyP1())
{
    val uID = bind(SongPropertyP1::uIDProperty)
    val name = bind(SongPropertyP1::nameProperty)
    val vocalist = bind(SongPropertyP1::vocalistProperty)
    val vocalistUID = bind(SongPropertyP1::vocalistUIDProperty)
    val producer = bind(SongPropertyP1::producerProperty)
    val producerUID = bind(SongPropertyP1::producerUIDProperty)
    val mixing = bind(SongPropertyP1::mixingProperty)
    val mixingUID = bind(SongPropertyP1::mixingUIDProperty)
    val mastering = bind(SongPropertyP1::masteringProperty)
    val masteringUID = bind(SongPropertyP1::masteringUIDProperty)
    val genre = bind(SongPropertyP1::genreProperty)
    val subgenre = bind(SongPropertyP1::subgenreProperty)
    val songLength = bind(SongPropertyP1::songLengthProperty)
    val vibe = bind(SongPropertyP1::vibeProperty)
    val songState = bind(SongPropertyP1::songStateProperty)
    val instruState = bind(SongPropertyP1::instruStateProperty)
    val lyricsState = bind(SongPropertyP1::lyricsStateProperty)
    val vocalsState = bind(SongPropertyP1::vocalsStateProperty)
    val mixingState = bind(SongPropertyP1::mixingStateProperty)
    val masteringState = bind(SongPropertyP1::masteringStateProperty)
    val isPromoted = bind(SongPropertyP1::isPromotedProperty)
    val distributed = bind(SongPropertyP1::distributedProperty)
    val isExclusiveRelease = bind(SongPropertyP1::isExclusiveReleaseProperty)
    val exclusiveChannel = bind(SongPropertyP1::exclusiveChannelProperty)
    val moneySpent = bind(SongPropertyP1::moneySpentProperty)
    val moneyGainedStreams = bind(SongPropertyP1::moneyGainedStreamsProperty)
    val moneyGainedSponsor = bind(SongPropertyP1::moneyGainedSponsorProperty)
}

class SongModelP2 : ItemViewModel<SongPropertyP2>(SongPropertyP2())
{
    val isPublic = bind(SongPropertyP2::isPublicProperty)
    val releaseDate = bind(SongPropertyP2::releaseDateProperty)
    val onSpotify = bind(SongPropertyP2::onSpotifyProperty)
    val onYouTube = bind(SongPropertyP2::onYouTubeProperty)
    val onSoundCloud = bind(SongPropertyP2::onSoundcloudProperty)
    val hasVisualizer = bind(SongPropertyP2::hasVisualizerProperty)
    val hasAnimeMV = bind(SongPropertyP2::hasAnimeMVProperty)
    val hasRealMV = bind(SongPropertyP2::hasRealMVProperty)
    val inEP = bind(SongPropertyP2::inEPProperty)
    val inAlbum = bind(SongPropertyP2::inAlbumProperty)
    val nameEP = bind(SongPropertyP2::nameEPProperty)
    val nameAlbum = bind(SongPropertyP2::nameAlbumProperty)
    val playsSpotify = bind(SongPropertyP2::playsSpotifyProperty)
    val playsYouTube = bind(SongPropertyP2::playsYouTubeProperty)
    val playsSoundCloud = bind(SongPropertyP2::playsSoundCloudProperty)
    val coVocalist1 = bind(SongPropertyP2::coVocalist1Property)
    val coVocalist1UID = bind(SongPropertyP2::coVocalist1UIDProperty)
    val coVocalist2 = bind(SongPropertyP2::coVocalist2Property)
    val coVocalist2UID = bind(SongPropertyP2::coVocalist2UIDProperty)
    val coProducer1 = bind(SongPropertyP2::coProducer1Property)
    val coProducer1UID = bind(SongPropertyP2::coProducer1UIDProperty)
    val coProducer2 = bind(SongPropertyP2::coProducer2Property)
    val coProducer2UID = bind(SongPropertyP2::coProducer2UIDProperty)
    val isProtected = bind(SongPropertyP2::isProtectedProperty)
    val containsCRMaterial = bind(SongPropertyP2::containsCRMaterialProperty)
    val containsExplicitLyrics = bind(SongPropertyP2::containsExplicitLyricsProperty)
    val inspiredByArtist = bind(SongPropertyP2::inspiredByArtistProperty)
    val inspiredBySong = bind(SongPropertyP2::inspiredBySongProperty)
    val dawUsed = bind(SongPropertyP2::dawUsedProperty)
    val micUsed = bind(SongPropertyP2::micUsedProperty)
    val comment = bind(SongPropertyP2::commentProperty)
}

fun getSongPropertyP1FromSong(song: Song): SongPropertyP1
{
    val songPropertyP1 = SongPropertyP1()
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    songPropertyP1.uID = song.uID
    songPropertyP1.name = song.name
    songPropertyP1.vocalist = song.vocalist
    songPropertyP1.vocalistUID = song.vocalistUID
    songPropertyP1.producer = song.producer
    songPropertyP1.producerUID = song.producerUID
    songPropertyP1.mixing = song.mixing
    songPropertyP1.mixingUID = song.mixingUID
    songPropertyP1.mastering = song.mastering
    songPropertyP1.masteringUID = song.masteringUID
    songPropertyP1.genre = song.genre
    songPropertyP1.subgenre = song.subgenre
    songPropertyP1.songLength = song.songLength
    songPropertyP1.vibe = song.vibe
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    songPropertyP1.songState = song.songState
    songPropertyP1.instruState = song.instruState
    songPropertyP1.lyricsState = song.lyricsState
    songPropertyP1.vocalsState = song.vocalsState
    songPropertyP1.mixingState = song.mixingState
    songPropertyP1.masteringState = song.masteringState
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    songPropertyP1.isPromoted = song.isPromoted
    songPropertyP1.distributed = song.distributed
    songPropertyP1.isExclusiveRelease = song.isExclusiveRelease
    songPropertyP1.exclusiveChannel = song.exclusiveChannel
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    songPropertyP1.moneySpent = song.moneySpent
    songPropertyP1.moneyGainedStreams = song.moneyGainedStreams
    songPropertyP1.moneyGainedSponsor = song.moneyGainedSponsor
    return songPropertyP1
}

fun getSongPropertyP2FromSong(song: Song): SongPropertyP2
{
    val songPropertyP2 = SongPropertyP2()
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    songPropertyP2.isPublic = song.isPublic
    songPropertyP2.releaseDate = LocalDate.parse(song.releaseDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    songPropertyP2.onSpotify = song.onSpotify
    songPropertyP2.onYouTube = song.onYouTube
    songPropertyP2.onSoundCloud = song.onSoundCloud
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    songPropertyP2.hasVisualizer = song.hasVisualizer
    songPropertyP2.hasAnimeMV = song.hasAnimeMV
    songPropertyP2.hasRealMV = song.hasRealMV
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    songPropertyP2.inEP = song.inEP
    songPropertyP2.inAlbum = song.inAlbum
    songPropertyP2.nameEP = song.nameEP
    songPropertyP2.nameAlbum = song.nameAlbum
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    songPropertyP2.playsSpotify = song.playsSpotify
    songPropertyP2.playsYouTube = song.playsYouTube
    songPropertyP2.playsSoundCloud = song.playsSoundCloud
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    songPropertyP2.coVocalist1 = song.coVocalist1
    songPropertyP2.coVocalist1UID = song.coVocalist1UID
    songPropertyP2.coVocalist2 = song.coVocalist2
    songPropertyP2.coVocalist2UID = song.coVocalist2UID
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    songPropertyP2.coProducer1 = song.coProducer1
    songPropertyP2.coProducer1UID = song.coProducer1UID
    songPropertyP2.coProducer2 = song.coProducer2
    songPropertyP2.coProducer2UID = song.coProducer2UID
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    songPropertyP2.isProtected = song.isProtected
    songPropertyP2.containsCRMaterial = song.containsCRMaterial
    songPropertyP2.containsExplicitLyrics = song.containsExplicitLyrics
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    songPropertyP2.inspiredByArtist = song.inspiredByArtist
    songPropertyP2.inspiredBySong = song.inspiredBySong
    songPropertyP2.dawUsed = song.dawUsed
    songPropertyP2.micUsed = song.micUsed
    songPropertyP2.comment = song.comment
    return songPropertyP2
}

fun getSongFromPropertyP1(songPropertyP1: SongPropertyP1): Song
{
    val song = Song(-1, songPropertyP1.name)
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    song.uID = songPropertyP1.uID
    song.vocalist = songPropertyP1.vocalist
    song.vocalistUID = songPropertyP1.vocalistUID
    song.producer = songPropertyP1.producer
    song.producerUID = songPropertyP1.producerUID
    song.mixing = songPropertyP1.mixing
    song.mixingUID = songPropertyP1.mixingUID
    song.mastering = songPropertyP1.mastering
    song.masteringUID = songPropertyP1.masteringUID
    song.genre = songPropertyP1.genre
    song.subgenre = songPropertyP1.subgenre
    song.songLength = songPropertyP1.songLength
    song.vibe = songPropertyP1.vibe
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    song.songState = songPropertyP1.songState
    song.instruState = songPropertyP1.instruState
    song.lyricsState = songPropertyP1.lyricsState
    song.vocalsState = songPropertyP1.vocalsState
    song.mixingState = songPropertyP1.mixingState
    song.masteringState = songPropertyP1.masteringState
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    song.isPromoted = songPropertyP1.isPromoted
    song.distributed = songPropertyP1.distributed
    song.isExclusiveRelease = songPropertyP1.isExclusiveRelease
    song.exclusiveChannel = songPropertyP1.exclusiveChannel
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    song.moneySpent = songPropertyP1.moneySpent
    song.moneyGainedStreams = songPropertyP1.moneyGainedStreams
    song.moneyGainedSponsor = songPropertyP1.moneyGainedSponsor
    return song
}

fun getSongFromPropertyP2(song: Song, songPropertyP2: SongPropertyP2): Song
{
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    song.isPublic = songPropertyP2.isPublic
    song.releaseDate = songPropertyP2.releaseDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    song.onSpotify = songPropertyP2.onSpotify
    song.onYouTube = songPropertyP2.onYouTube
    song.onSoundCloud = songPropertyP2.onSoundCloud
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    song.hasVisualizer = songPropertyP2.hasVisualizer
    song.hasAnimeMV = songPropertyP2.hasAnimeMV
    song.hasRealMV = songPropertyP2.hasRealMV
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    song.inEP = songPropertyP2.inEP
    song.inAlbum = songPropertyP2.inAlbum
    song.nameEP = songPropertyP2.nameEP
    song.nameAlbum = songPropertyP2.nameAlbum
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    song.playsSpotify = songPropertyP2.playsSpotify
    song.playsYouTube = songPropertyP2.playsYouTube
    song.playsSoundCloud = songPropertyP2.playsSoundCloud
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    song.coVocalist1 = songPropertyP2.coVocalist1
    song.coVocalist1UID = songPropertyP2.coVocalist1UID
    song.coVocalist2 = songPropertyP2.coVocalist2
    song.coVocalist2UID = songPropertyP2.coVocalist2UID
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    song.coProducer1 = songPropertyP2.coProducer1
    song.coProducer1UID = songPropertyP2.coProducer1UID
    song.coProducer2 = songPropertyP2.coProducer2
    song.coProducer2UID = songPropertyP2.coProducer2UID
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    song.isProtected = songPropertyP2.isProtected
    song.containsCRMaterial = songPropertyP2.containsCRMaterial
    song.containsExplicitLyrics = songPropertyP2.containsExplicitLyrics
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    song.inspiredByArtist = songPropertyP2.inspiredByArtist
    song.inspiredBySong = songPropertyP2.inspiredBySong
    song.dawUsed = songPropertyP2.dawUsed
    song.micUsed = songPropertyP2.micUsed
    song.comment = songPropertyP2.comment
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
 */