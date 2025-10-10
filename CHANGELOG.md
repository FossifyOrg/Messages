# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2025-10-10
### Added
- Ability to save multiple attachments ([#75])
- Ability to select numbers that aren't starred when starting a new conversation ([#153])

### Changed
- Reordered menu options throughout the app
- Updated translations

### Fixed
- Fixed keyword blocking for MMS messages ([#99])
- Fixed contact number selection when adding members to a group ([#456])
- Fixed a glitch in pattern lock after incorrect attempts

## [1.3.0] - 2025-09-09
### Added
- Option to keep conversations archived ([#334])

### Changed
- Updated translations

## [1.2.3] - 2025-08-21
### Changed
- Updated translations

### Fixed
- Fixed stale/missing notification badge on some devices

## [1.2.2] - 2025-08-01
### Changed
- Updated translations

### Fixed
- Fixed inability to view messages when there is no SIM card ([#461])

## [1.2.1] - 2025-06-17
### Changed
- Preference category labels now use sentence case
- Updated translations

## [1.2.0] - 2025-06-04
### Added
- Conversation shortcuts ([#209])

### Changed
- Updated translations

## [1.1.7] - 2025-04-01
### Changed
- Added more translations

### Fixed
- Fixed incorrect cursor position when reopening the app ([#349])
- Fixed scrolling issue on conversation details screen ([#359])

## [1.1.6] - 2025-03-24
### Changed
- Other minor fixes and improvements
- Added more translations

### Removed
- Removed storage permission requirement ([#309])

### Fixed
- Fixed crash when viewing messages
- Fixes incorrect author name in group messages ([#180])

## [1.1.5] - 2025-02-02
### Changed
- Added more translations

### Fixed
- Fixed issue with third party intents ([#294])
- Fixed toast error when receiving MMS messages ([#287])
- Fixed RTL layout issue in threads ([#279])

## [1.1.4] - 2025-01-23
### Changed
- Added more translations

### Fixed
- Fixed issue with forwarding messages ([#288])

## [1.1.3] - 2025-01-05
### Changed
- Added more translations

### Fixed
- Fixed issues with conversation date update ([#225], [#274])

## [1.1.2] - 2025-01-05
### Changed
- Added more translations

### Fixed
- Fixed issues with conversation date update ([#225], [#274])

## [1.1.1] - 2025-01-04
### Changed
- Improved third party SMS/MMS intent parsing ([#217], [#243])
- Modified short code check to exclude emails ([#115])
- Other minor bug fixes and improvements
- Added more translations

### Fixed
- Fixed issue with messages draft deletion ([#13])
- Fixed multiple toast errors for MMS messages ([#70], [#262])
- Fixed some layout issues in message thread ([#135])

## [1.1.0] - 2024-12-27
### Changed
- Replaced checkboxes with switches
- Improved app lock logic and interface
- Other minor bug fixes and improvements
- Added more translations

### Removed
- Removed support for Android 7 and older versions

### Fixed
- Fixed various issues related to importing/exporting messages
- Fixed keyword blocking for MMS messages
- Fixed issue with messages draft deletion

## [1.0.1] - 2024-02-09
### Changed
- Minor bug fixes and improvements
- Added some translations

## [1.0.0] - 2024-01-24
### Added
- Initial release

[#13]: https://github.com/FossifyOrg/Messages/issues/13
[#70]: https://github.com/FossifyOrg/Messages/issues/70
[#75]: https://github.com/FossifyOrg/Messages/issues/75
[#99]: https://github.com/FossifyOrg/Messages/issues/99
[#115]: https://github.com/FossifyOrg/Messages/issues/115
[#135]: https://github.com/FossifyOrg/Messages/issues/135
[#153]: https://github.com/FossifyOrg/Messages/issues/153
[#180]: https://github.com/FossifyOrg/Messages/issues/180
[#209]: https://github.com/FossifyOrg/Messages/issues/209
[#217]: https://github.com/FossifyOrg/Messages/issues/217
[#225]: https://github.com/FossifyOrg/Messages/issues/225
[#243]: https://github.com/FossifyOrg/Messages/issues/243
[#262]: https://github.com/FossifyOrg/Messages/issues/262
[#274]: https://github.com/FossifyOrg/Messages/issues/274
[#279]: https://github.com/FossifyOrg/Messages/issues/279
[#287]: https://github.com/FossifyOrg/Messages/issues/287
[#288]: https://github.com/FossifyOrg/Messages/issues/288
[#294]: https://github.com/FossifyOrg/Messages/issues/294
[#309]: https://github.com/FossifyOrg/Messages/issues/309
[#334]: https://github.com/FossifyOrg/Messages/issues/334
[#349]: https://github.com/FossifyOrg/Messages/issues/349
[#359]: https://github.com/FossifyOrg/Messages/issues/359
[#456]: https://github.com/FossifyOrg/Messages/issues/456
[#461]: https://github.com/FossifyOrg/Messages/issues/461

[Unreleased]: https://github.com/FossifyOrg/Messages/compare/1.4.0...HEAD
[1.4.0]: https://github.com/FossifyOrg/Messages/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/FossifyOrg/Messages/compare/1.2.3...1.3.0
[1.2.3]: https://github.com/FossifyOrg/Messages/compare/1.2.2...1.2.3
[1.2.2]: https://github.com/FossifyOrg/Messages/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/FossifyOrg/Messages/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/FossifyOrg/Messages/compare/1.1.7...1.2.0
[1.1.7]: https://github.com/FossifyOrg/Messages/compare/1.1.6...1.1.7
[1.1.6]: https://github.com/FossifyOrg/Messages/compare/1.1.5...1.1.6
[1.1.5]: https://github.com/FossifyOrg/Messages/compare/1.1.4...1.1.5
[1.1.4]: https://github.com/FossifyOrg/Messages/compare/1.1.3...1.1.4
[1.1.3]: https://github.com/FossifyOrg/Messages/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/FossifyOrg/Messages/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/FossifyOrg/Messages/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/FossifyOrg/Messages/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/FossifyOrg/Messages/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/FossifyOrg/Messages/releases/tag/1.0.0
