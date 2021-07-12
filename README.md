# CWO_ERP
CWO Enterprise Resource Planning Software using a custom self-written database. 
Using ANNs (Artificial Neural Networks) possible user inputs or other actions will be predicted and complex analytics and forecasts will be made possible.


## Modules
This section names and describes the modules that are currently implemented

### M1 Songs
This module is designed to store songs with all their metadata e.g. name, people that worked on it, streams/plays/views etc.
Later, APIs to streaming platforms like Spotify will be added to update the songs data (e.g. number of streams).

Next to inserting and looking up entries, there is also an analytics module inside, that allows the user to generate a pie chart,
showing the distribution of genres across all stored songs.

### M2 Contacts
This module is designed to store contacts.
Contacts can be loaded into the songs module by clicking on the [<] button next to contact fields to avoid typos and to load more data with less user actions.
