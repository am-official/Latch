# Latch 0.5.0

## Critical: monitor stays alive
- Dedicated **monitor notification channel** (ongoing service)
- **Watchdog alarm** every ~15 minutes restarts the service if the OS kills it
- Restarts after task removed / service destroy
- Settings: **Disable battery optimization** (strongly recommended)
- Do Not Disturb access shortcut for DND actions

## Clearer action fields
No more vague “Parameter” — each action has labeled fields, placeholders, and on/off pickers.

## New triggers
User unlocked · Battery saver on/off · App installed/removed · Storage low · Dock · Shutdown

## New actions
Media previous / play-pause · Alarm & ring volume · DND · Text-to-speech · Open app/system settings · HTTP GET

Leave at least one rule enabled and turn off battery optimization so automation keeps running overnight.
