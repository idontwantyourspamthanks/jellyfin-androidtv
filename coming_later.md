# Coming later

Ideas parked for later, not yet started.

## Kid users / toddler mode (parked 2026-06-21)

Designate certain Jellyfin users as "kids" and tailor the experience for them:

- **Strip libraries** — kid users only see an allow-listed subset of libraries (builds on the
  per-user library order/visibility we already have in `UserSettingPreferences.libraryDisplay` +
  `LibraryDisplayPreferences`).
- **Simplify the home page** — hide grown-up sections (e.g. Next Up, which is low value when the
  kids skip episode-to-episode anyway) and lean entirely on Continue Watching + Favourites + a big
  "Play something" button.
- **Maybe** a lock so a toddler can't wander into Settings / switch user / adult libraries.

Open questions:
- Where does "this user is a kid" live? A local per-user flag (device) vs something server-side.
  Jellyfin has parental-control ratings per user, but this is about *our* UI shaping, so probably a
  local flag keyed by user id.
- Is "kid mode" a property of the *user* or a separate *mode* you toggle?

Relates to [[home-drawer-feature]] (per-user library visibility already exists) and the home-page
rework (Favourites row + Play something + dropping Next Up/Latest/Live TV).
