package modules.mx.gui

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import modules.mx.logic.MXUserManager
import modules.mx.misc.MXUser
import modules.mx.token
import tornadofx.*

class MXGUser(user: MXUser) : Fragment("User")
{
    private val userManager: MXUserManager by inject()

    //ToDo: Replace this mess with a UserModel (binds)
    private val passwordProperty = SimpleStringProperty()
    private val usernameProperty = SimpleStringProperty()
    private val canAccessMX = SimpleBooleanProperty()
    private val canAccessM1Songs = SimpleBooleanProperty()
    private val canAccessM2Contacts = SimpleBooleanProperty()

    private val originalUser = user.copy()
    override val root = form {
        usernameProperty.value = user.username
        passwordProperty.value = userManager.decrypt(user.password, token)
        canAccessMX.value = user.canAccessMX
        canAccessM1Songs.value = user.canAccessM1Song
        canAccessM2Contacts.value = user.canAccessM2Contact
        fieldset("Credentials") {
            field("Username") { textfield(usernameProperty) }
            field("Password") { textfield(passwordProperty) }
        }
        fieldset("Rights (Attention! Changes to rights are active only after a restart!)") {
            fieldset("Access to...") {
                field("MX") { checkbox("", canAccessMX) }
                field("M1Songs") { checkbox("", canAccessM1Songs) }
                field("M2Contacts") { checkbox("", canAccessM2Contacts) }
            }
        }
        button("Save") {
            shortcut("Enter")
            action {
                user.username = usernameProperty.value
                user.password = userManager.encrypt(passwordProperty.value, token)
                user.canAccessMX = canAccessMX.value
                user.canAccessM1Song = canAccessM1Songs.value
                user.canAccessM2Contact = canAccessM2Contacts.value
                userManager.updateUser(user, originalUser)
                close()
            }
        }
    }
}