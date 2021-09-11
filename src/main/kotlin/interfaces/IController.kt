package interfaces

import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
interface IController: IModule {
    fun searchEntry()
    fun saveEntry()
    fun newEntry()
    fun showEntry(entry: IEntry)
}