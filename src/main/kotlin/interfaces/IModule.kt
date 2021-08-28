package interfaces

interface IModule {
    fun moduleNameLong(): String
    fun module(): String
    fun getApiUrl() = "http://${modules.mx.serverIPAddressGlobal}/api/${module().lowercase()}/"
}