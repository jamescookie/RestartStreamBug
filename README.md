Turns out this wasn't a bug, but a problem with my configuration (which didn't need to be changed in this project??)

Basically you need `kafka.streams.<stream-names>.session.timeout.ms` to be as low as possible (so `kafka.server.Defaults.GroupMinSessionTimeoutMs() + 1`)
