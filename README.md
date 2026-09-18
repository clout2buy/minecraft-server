# MistiqueCraft — Paper 1.20.1

Push to `main` and the server picks it up within 60 seconds.

| folder     | what happens on push                                            |
|------------|-----------------------------------------------------------------|
| `plugins/` | every `.jar` here is synced to the server (removed = uninstalled) and the server restarts |
| `config/`  | files here are copied into `plugins/<Name>/` on the server (e.g. `config/Essentials/config.yml`) |

Client versions: anyone 1.8 → latest can join (ViaVersion + ViaBackwards + ViaRewind).

Rules: only drop jars built for **Paper/Spigot 1.20.1**. A jar that crashes the server gets the
last commit reverted automatically and you'll see it in `#minecraft`.
