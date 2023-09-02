# Tablist

## Global player list

> referencing config.json -> global-player-list

The global player list will synchronize the tablist across the network

## Placeholders

You can integrate placeholders by typing `<tag>`<br/>
For example to display the max amount of players use `<global_max>`

| Tag                   | Description                                                     |
|-----------------------|-----------------------------------------------------------------|
| global_online         | the number of all players connected to the proxy                |
| global_max            | the max player count of the proxy                               |
| current_server_online | the number of all players connected to the current server       |
| current_server        | the name of the current server                                  |
| current_group_online  | the number of all players connected to the current server group |
| current_group         | the name of the current server group                            |
| ping                  | the current ping of the player                                  |
| server                | the name of the server                                          |
| domain                | the domain of the server                                        |
| discord               | the discord of the server                                       |

## Server/Tablist groups

> referencing config.json -> groups and server-groups

To add a server to a tablist group use `server`, `group`

The key represents the server and the value represents the tablist group the server should be associated with

```json
{
  "citybuild": "global",
  "creative": "global",
  "lobby-1": "lobby",
  "lobby-2": "lobby",
  "lobby-3": "lobby"
}
```

In this example, all the lobbies will show the tablist, that is configured under `groups` -> `lobby`<br/>
If `groups` -> `lobby` -> `hide-players` is enabled the players connected to the lobby will not be
visible to players connected to other server groups

## Server name overrides

> referencing config.json -> server-names

To override the name of a server use `server`, `name`

The key represents the server and the value represents the name that should be displayed instead

```json
{
  "citybuild": "CityBuild",
  "lobby-1": "Lobby",
  "lobby-2": "Lobby",
  "lobby-3": "Lobby"
}
```

Instead of `citybuild`, `CityBuild` will be displayed<br/>
Instead of `lobby-1`, `lobby-2` or `lobby-3`, `Lobby` will be displayed