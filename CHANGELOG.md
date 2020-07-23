# Bound Totems Changelog
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 7-23-2020
### Fixed
- Gave generated loot pools unique names to prevent potential NPEs.

## [1.3.0] - 6-3-2020
### Added
- Bound Compass item that shows a player the nearest totem shelf bound to them in their current dimension.

## [1.2.0] - 6-1-2020
### Added
- Compatibility with Hardcore Revival mod. (Resolves [Issue #2](https://github.com/Phylogeny/BoundTotems/issues/2))
- Configurable max number of simultaneously bound totem shelves. The minimum is 1, max is Integer.MAX_VALUE, and the default is 10. If an additional shelf is bound once the max has been reached, a randomly selected currently bound shelf will be struck with lightning and converted into a useless charred shelf. Items can be taken from a charred shelf but cannot be placed in it, and any totems it will have no effect as long as they remain in it. (Implements [Issue #1](https://github.com/Phylogeny/BoundTotems/issues/1))

## [1.1.0] - 5-21-2020
### Changed
- Totem shelves now:
    - Interact with comparators
    - Can be waterlogged

## [1.0.0] - 5-18-2020
### Added
- Initial release

---
# Info
Types of changes include: 
- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.