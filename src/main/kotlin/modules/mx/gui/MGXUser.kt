package modules.mx.gui

import io.ktor.util.*
import javafx.scene.paint.Color
import kotlinx.serialization.ExperimentalSerializationApi
import modules.mx.MXCredentials
import modules.mx.MXUser
import modules.mx.logic.MXUserManager
import modules.mx.misc.MXUserModel
import modules.mx.misc.getUserFromUserProperty
import modules.mx.misc.getUserPropertyFromUser
import modules.mx.rightButtonsWidth
import modules.mx.token
import tornadofx.*

@ExperimentalSerializationApi
@InternalAPI
class MGXUser(user: MXUser, credentials: MXCredentials) : Fragment("User") {
    private val userManager: MXUserManager by inject()
    private val userModel = MXUserModel(getUserPropertyFromUser(user))
    private val originalUser = user.copy()
    override val root = form {
        userModel.password.value = userManager.decrypt(userModel.password.value, token)
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
            }
        }
        button("Save") {
            shortcut("Enter")
            action {
                userModel.password.value = userManager.encrypt(userModel.password.value, token)
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