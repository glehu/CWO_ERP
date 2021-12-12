package modules.mx.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.Credentials
import modules.mx.User
import modules.mx.logic.UserManager
import modules.mx.logic.decryptAES
import modules.mx.logic.encryptAES
import modules.mx.misc.MXUserModel
import modules.mx.misc.getUserFromUserProperty
import modules.mx.misc.getUserPropertyFromUser
import modules.mx.rightButtonsWidth
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class GUser(user: User, credentials: Credentials) : Fragment("User") {
    private val userManager: UserManager by inject()
    private val userModel = MXUserModel(getUserPropertyFromUser(user))
    private val originalUser = user.copy()
    override val root = form {
        userModel.password.value = decryptAES(userModel.password.value)
        fieldset("Credentials") {
            field("Username") { textfield(userModel.username).required() }
            field("Password") { textfield(userModel.password).required() }
        }
        fieldset("Rights (Attention! Changes to rights are active only after a restart!)") {
            fieldset("Access to...") {
                field("MX") { checkbox("", userModel.canAccessMX) }
                field("M1Songs") { checkbox("", userModel.canAccessM1) }
                field("M2Contacts") { checkbox("", userModel.canAccessM2) }
                field("M3Invoice") { checkbox("", userModel.canAccessM3) }
                field("M4Inventory") { checkbox("", userModel.canAccessM4) }
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
