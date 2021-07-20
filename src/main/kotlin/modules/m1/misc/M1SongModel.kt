package modules.m1.misc

import javafx.beans.property.*
import modules.m1.Song
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SongProperty
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
    var coVocalist1: String by coVocalist1Property
    val coVocalist2Property = SimpleStringProperty("?")
    var coVocalist2: String by coVocalist2Property

    //----------------------------------v
    //---------- Collab Data -----------|
    //----------------------------------^
    val coProducer1Property = SimpleStringProperty("?")
    var coProducer1: String by coProducer1Property
    val coProducer2Property = SimpleStringProperty("?")
    var coProducer2: String by coProducer2Property

    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    private val isProtectedProperty = SimpleBooleanProperty(false)
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
    val byteSizeProperty = SimpleLongProperty(0L)
    var byteSize by byteSizeProperty
}

class SongModel : ItemViewModel<SongProperty>(SongProperty())
{
    val uID = bind(SongProperty::uIDProperty)
    val name = bind(SongProperty::nameProperty)
    val vocalist = bind(SongProperty::vocalistProperty)
    val vocalistUID = bind(SongProperty::vocalistUIDProperty)
    val producer = bind(SongProperty::producerProperty)
    val producerUID = bind(SongProperty::producerUIDProperty)
    val mixing = bind(SongProperty::mixingProperty)
    val mixingUID = bind(SongProperty::mixingUIDProperty)
    val mastering = bind(SongProperty::masteringProperty)
    val masteringUID = bind(SongProperty::masteringUIDProperty)
    val genre = bind(SongProperty::genreProperty)
    val subgenre = bind(SongProperty::subgenreProperty)
    val songLength = bind(SongProperty::songLengthProperty)
    val vibe = bind(SongProperty::vibeProperty)
    val songState = bind(SongProperty::songStateProperty)
    val instruState = bind(SongProperty::instruStateProperty)
    val lyricsState = bind(SongProperty::lyricsStateProperty)
    val vocalsState = bind(SongProperty::vocalsStateProperty)
    val mixingState = bind(SongProperty::mixingStateProperty)
    val masteringState = bind(SongProperty::masteringStateProperty)
    val isPromoted = bind(SongProperty::isPromotedProperty)
    val distributed = bind(SongProperty::distributedProperty)
    val isExclusiveRelease = bind(SongProperty::isExclusiveReleaseProperty)
    val exclusiveChannel = bind(SongProperty::exclusiveChannelProperty)
    val moneySpent = bind(SongProperty::moneySpentProperty)
    val moneyGainedStreams = bind(SongProperty::moneyGainedStreamsProperty)
    val moneyGainedSponsor = bind(SongProperty::moneyGainedSponsorProperty)
    val isPublic = bind(SongProperty::isPublicProperty)
    val releaseDate = bind(SongProperty::releaseDateProperty)
    val onSpotify = bind(SongProperty::onSpotifyProperty)
    val onYouTube = bind(SongProperty::onYouTubeProperty)
    val onSoundCloud = bind(SongProperty::onSoundcloudProperty)
    val hasVisualizer = bind(SongProperty::hasVisualizerProperty)
    val hasAnimeMV = bind(SongProperty::hasAnimeMVProperty)
    val hasRealMV = bind(SongProperty::hasRealMVProperty)
    val inEP = bind(SongProperty::inEPProperty)
    val inAlbum = bind(SongProperty::inAlbumProperty)
    val nameEP = bind(SongProperty::nameEPProperty)
    val nameAlbum = bind(SongProperty::nameAlbumProperty)
    val playsSpotify = bind(SongProperty::playsSpotifyProperty)
    val playsYouTube = bind(SongProperty::playsYouTubeProperty)
    val playsSoundCloud = bind(SongProperty::playsSoundCloudProperty)
    val coVocalist1 = bind(SongProperty::coVocalist1Property)
    val coVocalist2 = bind(SongProperty::coVocalist2Property)
    val coProducer1 = bind(SongProperty::coProducer1Property)
    val coProducer2 = bind(SongProperty::coProducer2Property)
    val isProtected = bind(SongProperty::isPromotedProperty)
    val containsCRMaterial = bind(SongProperty::containsCRMaterialProperty)
    val containsExplicitLyrics = bind(SongProperty::containsExplicitLyricsProperty)
    val inspiredByArtist = bind(SongProperty::inspiredByArtistProperty)
    val inspiredBySong = bind(SongProperty::inspiredBySongProperty)
    val dawUsed = bind(SongProperty::dawUsedProperty)
    val micUsed = bind(SongProperty::micUsedProperty)
    val comment = bind(SongProperty::commentProperty)
    val byteSize = bind(SongProperty::byteSizeProperty)
}

fun getSongPropertyFromSong(song: Song): SongProperty
{
    val songProperty = SongProperty()
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    songProperty.uID = song.uID
    songProperty.name = song.name
    songProperty.vocalist = song.vocalist
    songProperty.vocalistUID = song.vocalistUID
    songProperty.producer = song.producer
    songProperty.producerUID = song.producerUID
    songProperty.mixing = song.mixing
    songProperty.mixingUID = song.mixingUID
    songProperty.mastering = song.mastering
    songProperty.masteringUID = song.masteringUID
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
    song.uID = songProperty.uID
    song.vocalist = songProperty.vocalist
    song.vocalistUID = songProperty.vocalistUID
    song.producer = songProperty.producer
    song.producerUID = songProperty.producerUID
    song.mixing = songProperty.mixing
    song.mixingUID = songProperty.mixingUID
    song.mastering = songProperty.mastering
    song.masteringUID = songProperty.masteringUID
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