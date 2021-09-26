# CWO ERP
CWO Enterprise Resource Planning Software using a custom self-written recursive relational object database. 

## First steps
If you run the file `CWO_ERP.jar`, that is included in every release, for the first time, 
it will open the preferences' module for you to configure the software's settings.

```
{
    "encryption key": "<encryption key [default: random generated token]>",
    "data path": "<path where the data ordner sits [default: path of jar file]>",
    "max search results": <amount [default: 2000]>,
    "difference from utc in hours": <difference in hours [default: 0]>,
    "client": <true/false [default: false]>,
    "server ip address": "<IPv4 address>"
}
```

If the software should run in server mode, the "client" field should be set to **false**.
The server's IPv4 address can be found by opening the OS's command prompt and entering **ip config**.
The port the server is using is **8000**, so the IPv4 is your computer's IPv4 address + **8000**.

If the software should run in client mode, the "client" field should be set to **true**.
Following the instruction above, fill the server's IPv4 address into the "server ip address" field.

If you are ok with the configuration file you can proceed to log into the software. 
If it's the first time the software got executed, there will be a default user with following credentials:

Username: "**admin**" Password: "**admin**"

It is highly recommended changing the default user's password as it possesses all rights in the software, 
e.g. creating, editing and deleting users and full access to all modules.

If the software is running in client mode, enter the credentials provided by the system administrator.

## Modules
This section names and describes the modules that are currently implemented

#### M1 Songs
This module is designed to store songs with all their metadata,
e.g. name, people that worked on it, streams/plays/views etc.

Contacts can be loaded via the context menu, to avoid typos and to load more data with less user actions.
Contacts loaded this way are being kept in sync with their main module's data.

Next to inserting and looking up entries, there is also an analytics module, 
that allows the user to generate a pie chart, showing the distribution of genres across all stored songs.

#### M2 Contacts
This module is designed to store contacts. Contacts can be loaded into songs and invoices.

Next to inserting and looking up entries, there is also an analytics module, 
that allows the user to generate a pie chart, showing the distribution of cities across all stored contacts.

Right clicking the "Sales" or "Expenses" fields of a contact enable the user to look up invoices of this contact.

#### M3 Invoices
This module is designed to store invoice data, e.g. income and expenses. 
Positions (items, services) from the M4 Inventory module can be added to the basket via the "Load Item" button.
With "Add Position" you can also an empty invoice position for custom temporary items.
Amount and price of positions can be edited inside the table by double-clicking the cells containing the values and confirming by hitting enter.

Contacts can be loaded via the context menu, to avoid typos and to load more data with less user actions.
Contacts loaded this way are being kept in sync with their main module's data.

Loading a buyer contact sets the price category of this invoice according to the contact's data.

Clicking "Paid" or entering the invoice total price into the field "Paid" sets the amount of money that got already paid.
Upon processing an invoice by clicking "Process" the invoice gets set to "Finished".
In case a buyer and/or seller was loaded, they get the invoice paid amount stored as "Sales" or "Expenses", depending on their role in the invoice.

#### M4 Inventory
This module is designed to store items and services and specifying their prices.
The price categories are being managed in the M4 Price Categories module.

Entries of this module can be loaded into invoices.

#### M4 Price Categories
This module holds the price categories that can be loaded into contacts and that you find in items to set their price for different uses.

Loading a buyer contact sets the price category of an invoice according to the contact's price category.

#### MX Management
In this module the administrative tools lie. In summary:

* Users can be added, edited and removed in the user manager.
* User rights can be managed, controlling access to the modules.
* View the database's size and date + user logging of its last change.
* Reset a whole database with just one click (caution!)
* Update the database's indices via the "Update" button in case of a new data source.
* Spotify API token data + login link to be put in the browser.

#### API

Download your and other's spotify discography and have them be in the database.

#
# Database access via its own API

The software's data can be requested via API. All requests need to have valid credentials in the Authorization header. 
Authentication is being achieved via basic auth.

Currently, following API endpoints are available:

### server IPv4:8000/api/m1/
* entry/ {searchText} &type= {uid or name} &format=json

If used with type=uid the entry related to the uID provided in the search text will be sent back in json format.
If used with type=name all entries that match the search text will be sent back inside a json response with following format:

```
{
    total: Int,
    resultsList: ArrayList<String>
}
```

* indexselection
* getentrylock/ {uID}

### server IPv4:8000/api/m2/
* entry/ {searchText} &type= {uid or name} &format=json

If used with type=uid the entry related to the uID provided in the search text will be sent back in json format.
If used with type=name all entries that match the search text will be sent back inside a json response with following format:

```
{
    total: Int,
    resultsList: ArrayList<String>
}
```

* indexselection
* getentrylock/ {uID}

### server IPv4:8000/api/m3/
* entry/ {searchText} &type= {uid or name} &format=json

If used with type=uid the entry related to the uID provided in the search text will be sent back in json format.
If used with type=name all entries that match the search text will be sent back inside a json response with following format:

```
{
    total: Int,
    resultsList: ArrayList<String>
}
```

* indexselection
* getentrylock/ {uID}

# Things for the future:
+ A website and app solution for easy access from everywhere
+ A webshop integrated into the website solution to enable the user to sell services and products online and to promote work
+ Blockchain to reassure financial data integrity, e.g. verifying the process of selling a product and receiving a transaction.
+ Using ANNs (Artificial Neural Networks) possible user inputs or other actions will be predicted 
and complex analytics and forecasts will be made possible.
