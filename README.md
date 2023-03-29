# Collabo

Collabo is a peer-to-peer collaborative editing plugin for the IntelliJ IDE. The plugin allows a user to host a code editing session, sharing a source code file with all guests that join the session. Subsequently, all users can make concurrent code changes on their local copy that will get replicated for each session participant. 

Collabo has a peer-to-peer architecture, using the Publish/Subscribe pattern. The system is decentralized, as the state of the shared document is not stored on a server. Instead, each peer has a local copy of the document state. Peers communicate between each other through application events that contain information about operations that the other peers have performed on their local document copy. 

The operations received from other peers can be applied by each peer on their local copy using a CRDT algorithm that guarantees that, eventually, all copies will have the same state. 


---
## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Collabo"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/cherryDevBomb/Collabo/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
