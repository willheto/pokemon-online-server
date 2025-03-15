<h1>Pokemon MMO server</h1>

**early development**

## About the project

MMO game-engine/server written from scratch. 200ms tick-based game loop, optimized to support up to 1000+ players online (on the same chunk!) at the same time. Supports serving game assets, models, textures and more from server side. Also includes update server, login server, and register server. Chunking strategy is implemented, and because of that and custom encoding of packets, game remains playable even on 56kbps connection.

This repository also includes register server, update server and login server.

## Register server

Constains one simple /create-account endpoint.

## Update server

The game engine/server supports serving assets from server side. Players first connect to update server, which checks if current cache is stil valid,
or if there is an update available. If update is available, the update server packs and sends new assets to the client.

## Login server

Login server handles login, logout, count players and reset world requests.

## Getting Started

You need to have Docker, Java 23 and Gradle installed. 

If you're running windows, you can get started by running these two commands on powershell:

1. initializeDatabase.ps1
2. startServer.ps1

If not:

1. Run docker-compose up -d on root to initialize database container
2. Run gradle run --args="migrate" to run migrations
3. Run gradle run to spin up the server

## Technologies used

1. Java 21
2. Gradle
3. Docker
4. MariaDB
