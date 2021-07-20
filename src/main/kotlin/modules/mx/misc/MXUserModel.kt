package modules.mx.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class MXUserModelProperty
{
    //Credentials
    val usernameProperty = SimpleStringProperty("")
    var username: String by usernameProperty
    val passwordProperty = SimpleStringProperty("")
    var password: String by passwordProperty
    //Rights... Access to modules
    val canAccessMXProperty = SimpleBooleanProperty(false)
    var canAccessMX by canAccessMXProperty
    val canAccessM1Property = SimpleBooleanProperty(true)
    var canAccessM1 by canAccessM1Property
    val canAccessM2Property = SimpleBooleanProperty(true)
    var canAccessM2 by canAccessM2Property
}

class MXUserModel(user: MXUserModelProperty) : ItemViewModel<MXUserModelProperty>(user)
{
    val username = bind(MXUserModelProperty::usernameProperty)
    val password = bind(MXUserModelProperty::passwordProperty)
    val canAccessMX = bind(MXUserModelProperty::canAccessMXProperty)
    val canAccessM1 = bind(MXUserModelProperty::canAccessM1Property)
    val canAccessM2 = bind(MXUserModelProperty::canAccessM2Property)
}

fun getUserPropertyFromUser(user: MXUser): MXUserModelProperty
{
    val userProperty = MXUserModelProperty()
    userProperty.username = user.username
    userProperty.password = user.password
    userProperty.canAccessMX = user.canAccessMX
    userProperty.canAccessM1 = user.canAccessM1
    userProperty.canAccessM2 = user.canAccessM2
    return userProperty
}

fun getUserFromUserProperty(userProperty: MXUserModelProperty): MXUser
{
    val user = MXUser(userProperty.username, userProperty.password)
    user.canAccessMX = userProperty.canAccessMX
    user.canAccessM1 = userProperty.canAccessM1
    user.canAccessM2 = userProperty.canAccessM2
    return user
}