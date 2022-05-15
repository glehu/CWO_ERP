package modules.mx.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.Credentials
import modules.mx.User
import modules.mx.logic.UserCLIManager
import modules.mx.logic.decryptAES
import modules.mx.logic.encryptAES
import modules.mx.misc.UserModel
import modules.mx.misc.getUserFromUserProperty
import modules.mx.misc.getUserPropertyFromUser
import modules.mx.rightButtonsWidth
import tornadofx.Fragment
import tornadofx.action
import tornadofx.button
import tornadofx.checkbox
import tornadofx.field
import tornadofx.fieldset
import tornadofx.form
import tornadofx.required
import tornadofx.style
import tornadofx.textfield
import tornadofx.vboxConstraints

@ExperimentalSerializationApi
@InternalAPI
class GUser(user: User, credentials: Credentials) : Fragment("User") {
  private val userManager = UserCLIManager()
  private val userModel = UserModel(getUserPropertyFromUser(user))
  private val originalUser = user.copy()
  override val root = form {
    userModel.password.value = decryptAES(userModel.password.value)
    fieldset("Credentials") {
      field("Username") { textfield(userModel.username).required() }
      field("Password") { textfield(userModel.password).required() }
    }
    fieldset("Rights (Attention! Changes to rights are active only after a restart!)") {
      fieldset("Access to...") {
        field("Management") { checkbox("", userModel.canAccessManagement) }
        field("Discography") { checkbox("", userModel.canAccessDiscography) }
        field("Contacts") { checkbox("", userModel.canAccessContacts) }
        field("Invoices") { checkbox("", userModel.canAccessInvoices) }
        field("Inventory") { checkbox("", userModel.canAccessInventory) }
        field("Clarifier") { checkbox("", userModel.canAccessClarifier) }
        field("SnippetBase") { checkbox("", userModel.canAccessSnippetBase) }
      }
    }
    button("Save") {
      shortcut("Enter")
      action {
        userModel.password.value = encryptAES(userModel.password.value)
        userModel.commit()
        userManager.updateUser(getUserFromUserProperty(userModel.item), originalUser, credentials)
        close()
      }
      prefWidth = rightButtonsWidth
    }
    button("Delete") {
      prefWidth = rightButtonsWidth
      action {
        userManager.deleteUser(originalUser, credentials)
        close()
      }
      style { unsafe("-fx-base", Color.DARKRED) }
      vboxConstraints { marginTop = 25.0 }
    }
  }
}
