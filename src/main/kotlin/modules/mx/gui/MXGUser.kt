package modules.mx.gui

import javafx.beans.property.SimpleStringProperty
import modules.mx.getToken
import modules.mx.logic.MXPasswordManager
import modules.mx.misc.MXUser
import tornadofx.*

class MXGUser(user: MXUser): Fragment("User")
{
    private val passwordManager: MXPasswordManager by inject()
    private val passwordProperty = SimpleStringProperty()
    private var password by passwordProperty
    override val root = form {
        passwordProperty.value = passwordManager.decrypt(user.password, getToken())
        fieldset {
            field("Username") { textfield(user.username) }
            field("Password") { textfield(passwordProperty) }
        }
        button("Save") {
            action {
                user.password = passwordManager.encrypt(passwordProperty.value, getToken())
                passwordManager.updateUser(user)
                close()
            }
        }
    }
}