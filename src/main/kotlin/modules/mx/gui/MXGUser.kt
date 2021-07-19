package modules.mx.gui

import javafx.beans.property.SimpleStringProperty
import modules.mx.logic.MXPasswordManager
import modules.mx.misc.MXUser
import modules.mx.token
import tornadofx.*

class MXGUser(user: MXUser): Fragment("User")
{
    private val passwordManager: MXPasswordManager by inject()
    private val passwordProperty = SimpleStringProperty()
    private val usernameProperty = SimpleStringProperty()
    private val originalUser = user.copy()
    override val root = form {
        usernameProperty.value = user.username
        passwordProperty.value = passwordManager.decrypt(user.password, token)
        fieldset {
            field("Username") { textfield(usernameProperty) }
            field("Password") { textfield(passwordProperty) }
        }
        button("Save") {
            shortcut("Enter")
            action {
                user.username = usernameProperty.value
                user.password = passwordManager.encrypt(passwordProperty.value, token)
                passwordManager.updateUser(user, originalUser)
                close()
            }
        }
    }
}