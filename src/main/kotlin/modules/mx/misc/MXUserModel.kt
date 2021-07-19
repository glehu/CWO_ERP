package modules.mx.misc

import javafx.beans.property.SimpleStringProperty
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class UserProperty
{
    val usernameProperty = SimpleStringProperty()
    var username: String by usernameProperty
    val passwordProperty = SimpleStringProperty()
    var password: String by passwordProperty
}

class UserModel : ItemViewModel<UserProperty>(UserProperty())
{
    val username = bind(UserProperty::usernameProperty)
    val password = bind(UserProperty::passwordProperty)
}

fun getUserPropertyFromUser(user: MXUser): UserProperty
{
    val userProperty = UserProperty()
    userProperty.username = user.username
    userProperty.password = user.password
    return userProperty
}