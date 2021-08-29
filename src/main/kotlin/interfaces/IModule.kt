package interfaces

interface IModule {
    fun moduleNameLong(): String
    fun module(): String
    fun getServerUrl() = "http://${modules.mx.serverIPAddressGlobal}/"
    fun getApiUrl() = "${getServerUrl()}api/${module().lowercase()}/"
}