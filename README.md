***

# HerobrineHUD - Minecraft E-sports Broadcasting & Spectator System

HerobrineHUD is a mod tailor-made for **Minecraft E-sports**, live streaming, and professional broadcasting environments. Whether you are hosting a massive tournament or casual multiplayer minigames, this mod is designed to deliver a top-tier spectator experience.

Our philosophy: *Minecraft has never been short on incredibly fun games, but it has always lacked a system-level UI tool to make those games visually captivating for the audience.*

## ✨ Core Features

Currently, this mod provides a truly **out-of-the-box experience (zero config required)**. Simply install the mod on both the server and the broadcaster's client, and the client will seamlessly display real-time team information and player statuses. Vanilla `/team` groupings automatically sync directly to the broadcaster's HUD panel!

**Broadcaster / Operator Hotkeys:**
- **`H` Key**: Quickly toggle the HUD visibility.
- **`K` Key**: Open the HUD Management & Settings GUI. Here, you can toggle specific UI elements (such as armor slots, potion effects, etc.) and easily manage which players are displayed on the left and right sides of your screen.
- **Numpad Keys (1~9, 0)**: **Quick Camera Switching**. While in Spectator mode, simply press the numpad key corresponding to a player's slot number on your screen, and your camera will instantly and smoothly lock onto that player's POV!

## 🚀 Future Plans
This mod is built upon the concept of standard GSI (Game State Integration) protocols. I plan to reserve and open up the following interfaces to allow other mods to expand upon it, eventually building a massive broadcasting ecosystem:

* **Extensive GSI State Fields (Future Open API):** We will expose more game state data, allowing third-party mods to seamlessly inject their custom gamemode rules or specific data directly into the HUD rendering pipeline.
* **Real-time HTTP Broadcasting:** The server side will feature an interface to broadcast game data as a real-time HTTP service. This will allow external applications to receive live data and utilize Web technologies to build dedicated, third-party overlay interfaces (e.g., directly usable as an OBS Browser Source, eliminating the need for basic game window capture).

## 🗺️ Roadmap
* Migrate existing standalone Mixins to the highly compatible **Architectury API** to lay a solid cross-loader foundation (Fabric/Forge/NeoForge).
* Refactor the project architecture to embrace automated **Multi-version** builds.
* Integrate the **AUI framework**, bringing native Web technology into the game to flexibly render stunning, highly customizable Web-style UIs.
* Provide a rich UI theme framework alongside full **Theme Manager** support.

***
