# CWO ERP
CWO Enterprise Resource Planning Software using a custom self-written database. 

## First steps
If you run the file `CWO_ERP.jar`, that is included in every release, for the first time, 
it will open the preferences' module for you to configure the software's settings.

```
"encryption key": "<Key for encryption and decryption. It has to consist out of 16 numbers>",
"data path": "<The folder in which the database will be saved>",
"max search results": <The maximum amount of search result entries to be shown>,
"difference from utc in hours" <The difference from utc in hours>
```

If you are ok with the configuration file you can proceed to log into the software. 
If it's the first time the software got executed, there will be a default user with following credentials:

Username: "**admin**" password: "**admin**"

It is highly recommended changing the default user's password as it possesses all rights in the software, 
e.g. creating, editing and deleting users and full access to all modules.

## Modules
This section names and describes the modules that are currently implemented

#### M1 Songs
This module is designed to store songs with all their metadata,
e.g. name, people that worked on it, streams/plays/views etc.

Contacts can be loaded by clicking on the `[<]` button next to contact fields,
to avoid typos and to load more data with less user actions.

Next to inserting and looking up entries, there is also an analytics module, 
that allows the user to generate a pie chart, showing the distribution of genres across all stored songs.

#### M2 Contacts
This module is designed to store contacts.
Contacts can be loaded into songs and invoices.

Next to inserting and looking up entries, there is also an analytics module, 
that allows the user to generate a pie chart, showing the distribution of cities across all stored contacts.

#### M3 Invoices
This (as of Version `v0.2.0-alpha` very minimalistic) module is designed to store invoice data, e.g. income and expenses.
Contacts can be loaded by clicking on the `[<]` button next to contact fields,
to avoid typos and to load more data with less user actions.

## Things for the future:
+ APIs to streaming platforms like Spotify will be added to update the song's data (e.g. number of streams).
+ Using ANNs (Artificial Neural Networks) possible user inputs or other actions will be predicted 
and complex analytics and forecasts will be made possible.
