package modules.mx.misc

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.User
import tornadofx.ItemViewModel
import tornadofx.getValue
import tornadofx.setValue

class UserModelProperty {
  //Credentials
  val usernameProperty = SimpleStringProperty("")
  var username: String by usernameProperty
  val passwordProperty = SimpleStringProperty("")
  var password: String by passwordProperty

  //Rights... Access to modules
  val canAccessManagementProperty = SimpleBooleanProperty(false)
  var canAccessManagement by canAccessManagementProperty
  val canAccessDiscographyProperty = SimpleBooleanProperty(true)
  var canAccessDiscography by canAccessDiscographyProperty
  val canAccessContactsProperty = SimpleBooleanProperty(true)
  var canAccessContacts by canAccessContactsProperty
  val canAccessInvoicesProperty = SimpleBooleanProperty(true)
  var canAccessInvoices by canAccessInvoicesProperty
  val canAccessInventoryProperty = SimpleBooleanProperty(true)
  var canAccessInventory by canAccessInventoryProperty
}

class UserModel(user: UserModelProperty) : ItemViewModel<UserModelProperty>(user) {
  val username = bind(UserModelProperty::usernameProperty)
  val password = bind(UserModelProperty::passwordProperty)
  val canAccessManagement = bind(UserModelProperty::canAccessManagementProperty)
  val canAccessDiscography = bind(UserModelProperty::canAccessDiscographyProperty)
  val canAccessContacts = bind(UserModelProperty::canAccessContactsProperty)
  val canAccessInvoices = bind(UserModelProperty::canAccessInvoicesProperty)
  val canAccessInventory = bind(UserModelProperty::canAccessInventoryProperty)
}

fun getUserPropertyFromUser(user: User): UserModelProperty {
  val userProperty = UserModelProperty()
  userProperty.username = user.username
  userProperty.password = user.password
  userProperty.canAccessManagement = user.canAccessManagement
  userProperty.canAccessDiscography = user.canAccessDiscography
  userProperty.canAccessContacts = user.canAccessContacts
  userProperty.canAccessInvoices = user.canAccessInvoices
  userProperty.canAccessInventory = user.canAccessInventory
  return userProperty
}

fun getUserFromUserProperty(userProperty: UserModelProperty): User {
  val user = User(userProperty.username, userProperty.password)
  user.canAccessManagement = userProperty.canAccessManagement
  user.canAccessDiscography = userProperty.canAccessDiscography
  user.canAccessContacts = userProperty.canAccessContacts
  user.canAccessInvoices = userProperty.canAccessInvoices
  user.canAccessInventory = userProperty.canAccessInventory
  return user
}
