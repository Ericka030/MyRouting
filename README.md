# MyRouting - Custom SDN Routing Module

## Overview
MyRouting is a **custom routing application** built for the Floodlight SDN Controller.  
It handles OpenFlow-enabled switches by processing packet events, computing shortest paths,  
and dynamically installing flow rules in a software-defined network.

## Extended Description
MyRouting dynamically reacts to network changes, recalculates shortest paths, and updates flow tables in real-time.  
Key features:
- Handles `PacketIn` events and installs flow rules on switches.
- Uses graph-based shortest path algorithms (Dijkstra) with priority queues and hash maps.
- Scalable design for multiple switches and hosts.
- Modular code for easy integration of new routing strategies.

## Features
- Shortest-path routing algorithms
- OpenFlow API integration (`OFFlowMod`, `OFMatch`, `OFActionOutput`)
- Dynamic flow installation and network monitoring
- Fault-tolerant and concurrent handling of packet events

## Tech Stack
- **Language:** Java  
- **Frameworks:** Floodlight SDN Controller  
- **Protocols:** OpenFlow  
- **Concepts:** Networking, Graph Algorithms, Systems Programming

## Skills Demonstrated
- Networking & routing protocols  
- Distributed systems & concurrency  
- Algorithm implementation (graph/shortest path)  
- Systems programming in Java  
- Documentation & version control (Git)

## Installation
1. Clone the repo:
   ```bash
   git clone https://github.com/Ericka030/MyRouting.git
   cd
Authors

Collaborators: Alex Jong A Kiem
