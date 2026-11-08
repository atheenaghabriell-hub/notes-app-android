# Notes App for Android

## Offline notes and synchronization

Notes are stored in the app's SQLite database first, so adding, editing, archiving,
restoring, and deleting notes does not require a connection. Unsynced changes are
queued locally and uploaded by WorkManager when a network is available. The app
also queues a sync when the dashboard or archive screen opens. Pending
non-deleted notes are sent together as a JSON array to `sync_notes.php`, which
returns the corresponding MySQL IDs for local notes.

The Android emulator uses `http://10.0.2.2/notes_app/` to reach XAMPP on the
development computer. For a physical device, change `BASE_URL` in
`NoteSyncManager.java` to the computer's reachable LAN address and keep the phone
and computer on the same network. The matching PHP API files must be installed
under the `notes_app` web directory.

Offline account creation and login are not supported: the existing registration
and login flow still requires the PHP server. After logging in, notes remain
available locally while the server is offline.

The PHP `create_note.php` endpoint creates the `note_sync_map` table on demand
for idempotent retries of offline-created notes. The MySQL account must have
permission to create that table, and the `notes` table must include the columns
used by the API (`user_id`, `title`, `content`, `category`, `status`, and
`create_at`).
