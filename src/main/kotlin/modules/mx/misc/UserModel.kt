package modules.mx.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.User
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class MXUserModelProperty {
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
    val canAccessM3Property = SimpleBooleanProperty(true)
    var canAccessM3 by canAccessM3Property
    val canAccessM4Property = SimpleBooleanProperty(true)
    var canAccessM4 by canAccessM4Property
}

class MXUserModel(user: MXUserModelProperty) : ItemViewModel<MXUserModelProperty>(user) {
    val username = bind(MXUserModelProperty::usernameProperty)
    val password = bind(MXUserModelProperty::passwordProperty)
    val canAccessMX = bind(MXUserModelProperty::canAccessMXProperty)
    val canAccessM1 = bind(MXUserModelProperty::canAccessM1Property)
    val canAccessM2 = bind(MXUserModelProperty::canAccessM2Property)
    val canAccessM3 = bind(MXUserModelProperty::canAccessM3Property)
    val canAccessM4 = bind(MXUserModelProperty::canAccessM4Property)
}

fun getUserPropertyFromUser(user: User): MXUserModelProperty {
    val userProperty = MXUserModelProperty()
    userProperty.username = user.username
    userProperty.password = user.password
    userProperty.canAccessMX = user.canAccessMX
    userProperty.canAccessM1 = user.canAccessM1
    userProperty.canAccessM2 = user.canAccessM2
    userProperty.canAccessM3 = user.canAccessM3
    userProperty.canAccessM4 = user.canAccessM4
    return userProperty
}

fun getUserFromUserProperty(userProperty: MXUserModelProperty): User {
    val user = User(userProperty.username, userProperty.password)
    user.canAccessMX = userProperty.canAccessMX
    user.canAccessM1 = userProperty.canAccessM1
    user.canAccessM2 = userProperty.canAccessM2
    user.canAccessM3 = userProperty.canAccessM3
    user.canAccessM4 = userProperty.canAccessM4
    return user
}
