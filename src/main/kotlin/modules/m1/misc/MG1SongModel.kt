package modules.m1

import javafx.beans.property.*
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate

class SongProperty
{
    val uniqueIDProperty = SimpleIntegerProperty(-1)
    var uniqueID by uniqueIDProperty
    val nameProperty = SimpleStringProperty()
    var name: String by nameProperty

    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    val vocalistProperty = SimpleStringProperty()
    var vocalist: String by vocalistProperty
    val producerProperty = SimpleStringProperty()
    var producer: String by producerProperty
    val mixingProperty = SimpleStringProperty("?")
    var mixing: String by mixingProperty
    val masteringProperty = SimpleStringProperty("?")
    var mastering: String by masteringProperty
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
    val uniqueID = bind(SongProperty::uniqueIDProperty)
    val name = bind(SongProperty::nameProperty)
    val vocalist = bind(SongProperty::vocalistProperty)
    val producer = bind(SongProperty::producerProperty)
    val mixing = bind(SongProperty::mixingProperty)
    val mastering = bind(SongProperty::masteringProperty)
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