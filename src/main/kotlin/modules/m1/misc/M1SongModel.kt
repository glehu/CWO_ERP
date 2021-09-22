@file:Suppress("DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode",
    "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode", "DuplicatedCode",
    "DuplicatedCode", "DuplicatedCode"
)

package modules.m1.misc

import javafx.beans.property.*
import kotlinx.serialization.ExperimentalSerializationApi
import modules.m1.M1Song
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SongPropertyMainData {
    //----------------------------------v
    //----------- Main Data ------------|
    //----------------------------------^
    val uIDProperty = SimpleIntegerProperty(-1)
    var uID by uIDProperty
    val nameProperty = SimpleStringProperty()
    var name: String by nameProperty
    val vocalistProperty = SimpleStringProperty("?")
    val vocalistUIDProperty = SimpleIntegerProperty(-1)
    var vocalist: String by vocalistProperty
    var vocalistUID: Int by vocalistUIDProperty
    val producerProperty = SimpleStringProperty("?")
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
    val typeProperty = SimpleStringProperty(getSongTypeList()[0])
    var type: String by typeProperty
    val genreProperty = SimpleStringProperty("?")
    var genre: String by genreProperty
    val subgenreProperty = SimpleStringProperty("?")
    var subgenre: String by subgenreProperty
    val songLengthProperty = SimpleStringProperty("??:??")
    var songLength: String by songLengthProperty
    val vibeProperty = SimpleStringProperty("?")
    var vibe: String by vibeProperty
}

class SongPropertyMainDataModel : ItemViewModel<SongPropertyMainData>() {
    val uID = bind(SongPropertyMainData::uIDProperty)
    val name = bind(SongPropertyMainData::nameProperty)
    val vocalist = bind(SongPropertyMainData::vocalistProperty)
    val vocalistUID = bind(SongPropertyMainData::vocalistUIDProperty)
    val producer = bind(SongPropertyMainData::producerProperty)
    val producerUID = bind(SongPropertyMainData::producerUIDProperty)
    val mixing = bind(SongPropertyMainData::mixingProperty)
    val mixingUID = bind(SongPropertyMainData::mixingUIDProperty)
    val mastering = bind(SongPropertyMainData::masteringProperty)
    val masteringUID = bind(SongPropertyMainData::masteringUIDProperty)
    val type = bind(SongPropertyMainData::typeProperty)
    val genre = bind(SongPropertyMainData::genreProperty)
    val subgenre = bind(SongPropertyMainData::subgenreProperty)
    val songLength = bind(SongPropertyMainData::songLengthProperty)
    val vibe = bind(SongPropertyMainData::vibeProperty)
}

class SongPropertyCompletionState {
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
}

class SongPropertyCompletionStateModel : ItemViewModel<SongPropertyCompletionState>() {
    val songState = bind(SongPropertyCompletionState::songStateProperty)
    val instruState = bind(SongPropertyCompletionState::instruStateProperty)
    val lyricsState = bind(SongPropertyCompletionState::lyricsStateProperty)
    val vocalsState = bind(SongPropertyCompletionState::vocalsStateProperty)
    val mixingState = bind(SongPropertyCompletionState::mixingStateProperty)
    val masteringState = bind(SongPropertyCompletionState::masteringStateProperty)
}

class SongPropertyPromotionData {
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
}

class SongPropertyPromotionDataModel : ItemViewModel<SongPropertyPromotionData>() {
    val isPromoted = bind(SongPropertyPromotionData::isPromotedProperty)
    val distributed = bind(SongPropertyPromotionData::distributedProperty)
    val isExclusiveRelease = bind(SongPropertyPromotionData::isExclusiveReleaseProperty)
    val exclusiveChannel = bind(SongPropertyPromotionData::exclusiveChannelProperty)
}

class SongPropertyFinancialData {
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

class SongPropertyFinancialDataModel : ItemViewModel<SongPropertyFinancialData>() {
    val moneySpent = bind(SongPropertyFinancialData::moneySpentProperty)
    val moneyGainedStreams = bind(SongPropertyFinancialData::moneyGainedStreamsProperty)
    val moneyGainedSponsor = bind(SongPropertyFinancialData::moneyGainedSponsorProperty)
}

class SongPropertyAvailabilityData {
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
}

class SongPropertyAvailabilityDataModel : ItemViewModel<SongPropertyAvailabilityData>() {
    val isPublic = bind(SongPropertyAvailabilityData::isPublicProperty)
    val releaseDate = bind(SongPropertyAvailabilityData::releaseDateProperty)
    val onSpotify = bind(SongPropertyAvailabilityData::onSpotifyProperty)
    val onYouTube = bind(SongPropertyAvailabilityData::onYouTubeProperty)
    val onSoundcloud = bind(SongPropertyAvailabilityData::onSoundcloudProperty)
}

class SongPropertyVisualizationData {
    //----------------------------------v
    //------- Visualization Data -------|
    //----------------------------------^
    val hasVisualizerProperty = SimpleBooleanProperty(false)
    var hasVisualizer by hasVisualizerProperty
    val hasAnimeMVProperty = SimpleBooleanProperty(false)
    var hasAnimeMV by hasAnimeMVProperty
    val hasRealMVProperty = SimpleBooleanProperty(false)
    var hasRealMV by hasRealMVProperty
}

class SongPropertyVisualizationDataModel : ItemViewModel<SongPropertyVisualizationData>() {
    val hasVisualizer = bind(SongPropertyVisualizationData::hasVisualizerProperty)
    val hasAnimeMV = bind(SongPropertyVisualizationData::hasAnimeMVProperty)
    val hasRealMV = bind(SongPropertyVisualizationData::hasRealMVProperty)
}

class SongPropertyAlbumEPData {
    //----------------------------------v
    //---------- Album/EP Data ---------|
    //----------------------------------^
    val inAlbumProperty = SimpleBooleanProperty(false)
    var inAlbum by inAlbumProperty
    val nameAlbumProperty = SimpleStringProperty("?")
    var nameAlbum: String by nameAlbumProperty
    val typeAlbumProperty = SimpleStringProperty("?")
    var typeAlbum: String by typeAlbumProperty
    val albumUIDProperty = SimpleIntegerProperty(-1)
    var albumUID: Int by albumUIDProperty
}

class SongPropertyAlbumEPDataModel : ItemViewModel<SongPropertyAlbumEPData>() {
    val inAlbum = bind(SongPropertyAlbumEPData::inAlbumProperty)
    val nameAlbum = bind(SongPropertyAlbumEPData::nameAlbumProperty)
    val typeAlbum = bind(SongPropertyAlbumEPData::typeAlbumProperty)
    val albumUID = bind(SongPropertyAlbumEPData::albumUIDProperty)
}

class SongPropertyStatisticsData {
    //----------------------------------v
    //-------- Statistics Data ---------|
    //----------------------------------^
    val playsSpotifyProperty = SimpleIntegerProperty(0)
    var playsSpotify by playsSpotifyProperty
    val playsYouTubeProperty = SimpleIntegerProperty(0)
    var playsYouTube by playsYouTubeProperty
    val playsSoundCloudProperty = SimpleIntegerProperty(0)
    var playsSoundCloud by playsSoundCloudProperty
}

class SongPropertyStatisticsDataModel : ItemViewModel<SongPropertyStatisticsData>() {
    val playsSpotify = bind(SongPropertyStatisticsData::playsSpotifyProperty)
    val playsYouTube = bind(SongPropertyStatisticsData::playsYouTubeProperty)
    val playsSoundCloud = bind(SongPropertyStatisticsData::playsSoundCloudProperty)
}

class SongPropertyCollaborationData {
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
}

class SongPropertyCollaborationDataModel : ItemViewModel<SongPropertyCollaborationData>() {
    val coVocalist1 = bind(SongPropertyCollaborationData::coVocalist1Property)
    val coVocalist1UID = bind(SongPropertyCollaborationData::coVocalist1UIDProperty)
    val coVocalist2 = bind(SongPropertyCollaborationData::coVocalist2Property)
    val coVocalist2UID = bind(SongPropertyCollaborationData::coVocalist2UIDProperty)
    val coProducer1 = bind(SongPropertyCollaborationData::coProducer1Property)
    val coProducer1UID = bind(SongPropertyCollaborationData::coProducer1UIDProperty)
    val coProducer2 = bind(SongPropertyCollaborationData::coProducer2Property)
    val coProducer2UID = bind(SongPropertyCollaborationData::coProducer2UIDProperty)
}


class SongPropertyCopyrightData {
    //----------------------------------v
    //--------- Copyright Data ---------|
    //----------------------------------^
    val isProtectedProperty = SimpleBooleanProperty(false)
    var isProtected by isProtectedProperty
    val containsCRMaterialProperty = SimpleBooleanProperty(false)
    var containsCRMaterial by containsCRMaterialProperty
    val containsExplicitLyricsProperty = SimpleBooleanProperty(false)
    var containsExplicitLyrics by containsExplicitLyricsProperty
}

class SongPropertyCopyrightDataModel : ItemViewModel<SongPropertyCopyrightData>() {
    val isProtected = bind(SongPropertyCopyrightData::isProtectedProperty)
    val containsCRMaterial = bind(SongPropertyCopyrightData::containsCRMaterialProperty)
    val containsExplicitLyrics = bind(SongPropertyCopyrightData::containsExplicitLyricsProperty)
}

class SongPropertyMiscData {
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

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    val spotifyIDProperty = SimpleStringProperty("?")
    var spotifyID: String by spotifyIDProperty
}

class SongPropertyMiscDataModel : ItemViewModel<SongPropertyMiscData>() {
    val inspiredByArtist = bind(SongPropertyMiscData::inspiredByArtistProperty)
    val inspiredBySong = bind(SongPropertyMiscData::inspiredBySongProperty)
    val dawUsed = bind(SongPropertyMiscData::dawUsedProperty)
    val micUsed = bind(SongPropertyMiscData::micUsedProperty)
    val comment = bind(SongPropertyMiscData::commentProperty)
    val spotifyID = bind(SongPropertyMiscData::spotifyIDProperty)
}

@ExperimentalSerializationApi
fun getSongPropertyMainData(song: M1Song): SongPropertyMainData {
    val songPropertyMainData = SongPropertyMainData()
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    songPropertyMainData.uID = song.uID
    songPropertyMainData.name = song.name
    songPropertyMainData.vocalist = song.vocalist
    songPropertyMainData.vocalistUID = song.vocalistUID
    songPropertyMainData.producer = song.producer
    songPropertyMainData.producerUID = song.producerUID
    songPropertyMainData.mixing = song.mixing
    songPropertyMainData.mixingUID = song.mixingUID
    songPropertyMainData.mastering = song.mastering
    songPropertyMainData.masteringUID = song.masteringUID
    songPropertyMainData.type = song.type
    songPropertyMainData.genre = song.genre
    songPropertyMainData.subgenre = song.subgenre
    songPropertyMainData.songLength = song.songLength
    songPropertyMainData.vibe = song.vibe
    return songPropertyMainData
}

@ExperimentalSerializationApi
fun getSongPropertyCompletionState(song: M1Song): SongPropertyCompletionState {
    val songPropertyCompletionState = SongPropertyCompletionState()
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    songPropertyCompletionState.songState = song.songState
    songPropertyCompletionState.instruState = song.instruState
    songPropertyCompletionState.lyricsState = song.lyricsState
    songPropertyCompletionState.vocalsState = song.vocalsState
    songPropertyCompletionState.mixingState = song.mixingState
    songPropertyCompletionState.masteringState = song.masteringState
    return songPropertyCompletionState
}

@ExperimentalSerializationApi
fun getSongPropertyPromotionData(song: M1Song): SongPropertyPromotionData {
    val songPropertyPromotionData = SongPropertyPromotionData()
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    songPropertyPromotionData.isPromoted = song.isPromoted
    songPropertyPromotionData.distributed = song.distributed
    songPropertyPromotionData.isExclusiveRelease = song.isExclusiveRelease
    songPropertyPromotionData.exclusiveChannel = song.exclusiveChannel
    return songPropertyPromotionData
}

@ExperimentalSerializationApi
fun getSongPropertyFinancialData(song: M1Song): SongPropertyFinancialData {
    val songPropertyFinancialData = SongPropertyFinancialData()
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    songPropertyFinancialData.moneySpent = song.moneySpent
    songPropertyFinancialData.moneyGainedStreams = song.moneyGainedStreams
    songPropertyFinancialData.moneyGainedSponsor = song.moneyGainedSponsor
    return songPropertyFinancialData
}

@ExperimentalSerializationApi
fun getSongPropertyAvailabilityData(song: M1Song): SongPropertyAvailabilityData {
    val songPropertyAvailabilityData = SongPropertyAvailabilityData()
    //For songModel to be serialized, it has to be inserted into song
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    songPropertyAvailabilityData.isPublic = song.isPublic
    songPropertyAvailabilityData.releaseDate =
        LocalDate.parse(song.releaseDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    songPropertyAvailabilityData.onSpotify = song.onSpotify
    songPropertyAvailabilityData.onYouTube = song.onYouTube
    songPropertyAvailabilityData.onSoundCloud = song.onSoundCloud
    return songPropertyAvailabilityData
}

@ExperimentalSerializationApi
fun getSongPropertyVisualizationData(song: M1Song): SongPropertyVisualizationData {
    val songPropertyVisualizationData = SongPropertyVisualizationData()
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    songPropertyVisualizationData.hasVisualizer = song.hasVisualizer
    songPropertyVisualizationData.hasAnimeMV = song.hasAnimeMV
    songPropertyVisualizationData.hasRealMV = song.hasRealMV
    return songPropertyVisualizationData
}

@ExperimentalSerializationApi
fun getSongPropertyAlbumEPData(song: M1Song): SongPropertyAlbumEPData {
    val songPropertyAlbumEPData = SongPropertyAlbumEPData()
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    songPropertyAlbumEPData.inAlbum = song.inAlbum
    songPropertyAlbumEPData.nameAlbum = song.nameAlbum
    songPropertyAlbumEPData.typeAlbum = song.typeAlbum
    songPropertyAlbumEPData.albumUID = song.albumUID
    return songPropertyAlbumEPData
}

@ExperimentalSerializationApi
fun getSongPropertyStatisticsData(song: M1Song): SongPropertyStatisticsData {
    val songPropertyP2 = SongPropertyStatisticsData()
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    songPropertyP2.playsSpotify = song.playsSpotify
    songPropertyP2.playsYouTube = song.playsYouTube
    songPropertyP2.playsSoundCloud = song.playsSoundCloud
    return songPropertyP2
}

@ExperimentalSerializationApi
fun getSongPropertyCollaborationData(song: M1Song): SongPropertyCollaborationData {
    val songPropertyCollaborationData = SongPropertyCollaborationData()
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    songPropertyCollaborationData.coVocalist1 = song.coVocalist1
    songPropertyCollaborationData.coVocalist1UID = song.coVocalist1UID
    songPropertyCollaborationData.coVocalist2 = song.coVocalist2
    songPropertyCollaborationData.coVocalist2UID = song.coVocalist2UID
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    songPropertyCollaborationData.coProducer1 = song.coProducer1
    songPropertyCollaborationData.coProducer1UID = song.coProducer1UID
    songPropertyCollaborationData.coProducer2 = song.coProducer2
    songPropertyCollaborationData.coProducer2UID = song.coProducer2UID
    return songPropertyCollaborationData
}

@ExperimentalSerializationApi
fun getSongPropertyCopyrightData(song: M1Song): SongPropertyCopyrightData {
    val songPropertyCopyrightData = SongPropertyCopyrightData()
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    songPropertyCopyrightData.isProtected = song.isProtected
    songPropertyCopyrightData.containsCRMaterial = song.containsCRMaterial
    songPropertyCopyrightData.containsExplicitLyrics = song.containsExplicitLyrics
    return songPropertyCopyrightData
}

@ExperimentalSerializationApi
fun getSongPropertyMiscData(song: M1Song): SongPropertyMiscData {
    val songPropertyMiscData = SongPropertyMiscData()
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    songPropertyMiscData.inspiredByArtist = song.inspiredByArtist
    songPropertyMiscData.inspiredBySong = song.inspiredBySong
    songPropertyMiscData.dawUsed = song.dawUsed
    songPropertyMiscData.micUsed = song.micUsed
    songPropertyMiscData.comment = song.comment

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    songPropertyMiscData.spotifyID = song.spotifyID
    return songPropertyMiscData
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyMainData: SongPropertyMainData): M1Song {
    //val song = Song(-1, songPropertyP1.name)
    //---------------------------------v
    //----------- Main Data -----------|
    //---------------------------------^
    song.uID = songPropertyMainData.uID
    song.name = songPropertyMainData.name
    song.vocalist = songPropertyMainData.vocalist
    song.vocalistUID = songPropertyMainData.vocalistUID
    song.producer = songPropertyMainData.producer
    song.producerUID = songPropertyMainData.producerUID
    song.mixing = songPropertyMainData.mixing
    song.mixingUID = songPropertyMainData.mixingUID
    song.mastering = songPropertyMainData.mastering
    song.masteringUID = songPropertyMainData.masteringUID
    song.type = songPropertyMainData.type
    song.genre = songPropertyMainData.genre
    song.subgenre = songPropertyMainData.subgenre
    song.songLength = songPropertyMainData.songLength
    song.vibe = songPropertyMainData.vibe
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyCompletionState: SongPropertyCompletionState): M1Song {
    //val song = Song(-1, songPropertyP1.name)
    //----------------------------------v
    //------- State of Completion ------|
    //----------------------------------^
    song.songState = songPropertyCompletionState.songState
    song.instruState = songPropertyCompletionState.instruState
    song.lyricsState = songPropertyCompletionState.lyricsState
    song.vocalsState = songPropertyCompletionState.vocalsState
    song.mixingState = songPropertyCompletionState.mixingState
    song.masteringState = songPropertyCompletionState.masteringState
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyPromotionData: SongPropertyPromotionData): M1Song {
    //val song = Song(-1, songPropertyP1.name)
    //---------------------------------v
    //--------- Promotion Data --------|
    //---------------------------------^
    song.isPromoted = songPropertyPromotionData.isPromoted
    song.distributed = songPropertyPromotionData.distributed
    song.isExclusiveRelease = songPropertyPromotionData.isExclusiveRelease
    song.exclusiveChannel = songPropertyPromotionData.exclusiveChannel
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyFinancialData: SongPropertyFinancialData): M1Song {
    //val song = Song(-1, songPropertyP1.name)
    //---------------------------------v
    //--------- Financial Data --------|
    //---------------------------------^
    song.moneySpent = songPropertyFinancialData.moneySpent
    song.moneyGainedStreams = songPropertyFinancialData.moneyGainedStreams
    song.moneyGainedSponsor = songPropertyFinancialData.moneyGainedSponsor
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyAvailabilityData: SongPropertyAvailabilityData): M1Song {
    //---------------------------------v
    //------- Availability Data -------|
    //---------------------------------^
    song.isPublic = songPropertyAvailabilityData.isPublic
    song.releaseDate = songPropertyAvailabilityData.releaseDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    song.onSpotify = songPropertyAvailabilityData.onSpotify
    song.onYouTube = songPropertyAvailabilityData.onYouTube
    song.onSoundCloud = songPropertyAvailabilityData.onSoundCloud
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyVisualizationData: SongPropertyVisualizationData): M1Song {
    //---------------------------------v
    //------- Visualization Data ------|
    //---------------------------------^
    song.hasVisualizer = songPropertyVisualizationData.hasVisualizer
    song.hasAnimeMV = songPropertyVisualizationData.hasAnimeMV
    song.hasRealMV = songPropertyVisualizationData.hasRealMV
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyAlbumEPData: SongPropertyAlbumEPData): M1Song {
    //---------------------------------v
    //---------- Album/EP Data --------|
    //---------------------------------^
    song.inAlbum = songPropertyAlbumEPData.inAlbum
    song.nameAlbum = songPropertyAlbumEPData.nameAlbum
    song.typeAlbum = songPropertyAlbumEPData.typeAlbum
    song.albumUID = songPropertyAlbumEPData.albumUID
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyStatisticsData: SongPropertyStatisticsData): M1Song {
    //---------------------------------v
    //-------- Statistics Data --------|
    //---------------------------------^
    song.playsSpotify = songPropertyStatisticsData.playsSpotify
    song.playsYouTube = songPropertyStatisticsData.playsYouTube
    song.playsSoundCloud = songPropertyStatisticsData.playsSoundCloud
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyCollaborationData: SongPropertyCollaborationData): M1Song {
    //---------------------------------v
    //---------- Feature Data ---------|
    //---------------------------------^
    song.coVocalist1 = songPropertyCollaborationData.coVocalist1
    song.coVocalist1UID = songPropertyCollaborationData.coVocalist1UID
    song.coVocalist2 = songPropertyCollaborationData.coVocalist2
    song.coVocalist2UID = songPropertyCollaborationData.coVocalist2UID
    //---------------------------------v
    //---------- Collab Data ----------|
    //---------------------------------^
    song.coProducer1 = songPropertyCollaborationData.coProducer1
    song.coProducer1UID = songPropertyCollaborationData.coProducer1UID
    song.coProducer2 = songPropertyCollaborationData.coProducer2
    song.coProducer2UID = songPropertyCollaborationData.coProducer2UID
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyCopyrightData: SongPropertyCopyrightData): M1Song {
    //---------------------------------v
    //--------- Copyright Data --------|
    //---------------------------------^
    song.isProtected = songPropertyCopyrightData.isProtected
    song.containsCRMaterial = songPropertyCopyrightData.containsCRMaterial
    song.containsExplicitLyrics = songPropertyCopyrightData.containsExplicitLyrics
    return song
}

@ExperimentalSerializationApi
fun getSongFromProperty(song: M1Song, songPropertyMiscData: SongPropertyMiscData): M1Song {
    //---------------------------------v
    //----------- Misc Data -----------|
    //---------------------------------^
    song.inspiredByArtist = songPropertyMiscData.inspiredByArtist
    song.inspiredBySong = songPropertyMiscData.inspiredBySong
    song.dawUsed = songPropertyMiscData.dawUsed
    song.micUsed = songPropertyMiscData.micUsed
    song.comment = songPropertyMiscData.comment

    //----------------------------------v
    //------------ API Data ------------|
    //----------------------------------^
    song.spotifyID = songPropertyMiscData.spotifyID
    return song
}