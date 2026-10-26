# Main Project Demo - final main build

Updated 26 October 2026 against main `4007cf6`. Dedicated branch: `demo/main-project-presentation`.

## Launch

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-game.ps1 -Demo
```

Requires JDK 17+. The demo compiles sources directly; normal launch retains main's Gradle workflow.
The presenter plays normally. Back/Restart/Next (F6/F7/F8) change scenes manually. F5 holds prepared cues, not combat. F10 exits.
Prepared pickup/hit cues normally run after 8 seconds; boss phase cues at 12/20 seconds. There is no scene deadline.

## Profile

Default: `.demo-profile/`, ignored by Git. Settings, credits, purchases and records survive restart and stay separate from normal saves.
For a clean rehearsal, add `-DemoProfile .\out\final-rehearsal-01`. Reuse that path to verify persistence.
Starting credits (1000) and ranking history are fixtures. Restart/Back do not undo purchases.

## Scene order

| # | Scene | Presenter action / explanation |
| --- | --- | --- |
| 1 | Welcome aboard | Frenchies / VFX / Audio: navigation, stars and menu sounds. |
| 2 | Prepare your mission | Frenchies: all settings in one menu. |
| 3 | Sound and music | Chanyoung: adjust BGM/SFX volumes, then press M to toggle mute. |
| 4 | Custom controls | Frenchies: select an action, press Space, then choose a new key. |
| 5 | Choose your difficulty | Frenchies / Octopus: Easy, Normal or Hard for campaign gameplay. |
| 6 | Retained settings | Reopen Controls and Gameplay; confirm cancel/duplicate rejection and retained choices. |
| 7 | Choose your ship | KimchiBaguette: compare speed, fire rate, lives and projectiles. |
| 8 | Customize your ship | Frenchies / VFX: use Left/Right to change the ship color. |
| 9 | Two ways to play | Octopus / Frenchies: campaign progression and endless survival. |
| 10 | Records before the mission | Chinese can fly: prepared top-ten baseline. Inspect bests and five achievement conditions. |
| 11 | First mission | Octopus / Friends / VFX / Audio: play using your selected ship. |
| 12 | Campaign HUD and floating points | Final HUD: stage, lives, score and Credits. Shoot an enemy; watch points and pending Credits update. |
| 13 | Speedster: movement and bursts | KimchiBaguette: speed 4, two-shot bursts and three lives. Unlock it in the Shop for normal selection. |
| 14 | Heavy Tank: spread shots | KimchiBaguette: speed 1, three projectiles and five lives. |
| 15 | Damage particles and flashing | VFX: staged hit at 8s; watch the life loss, red/green flash and player particles. |
| 16 | Third-stage entry: extra life | Prepared stage-3 entry with 3/4 lives: the final bonus-life rule grants the fourth heart. |
| 17 | Extra life is capped | Prepared stage-3 entry with Heavy Tank at 5/5 lives: the bonus cannot exceed the cap. |
| 18 | Enemy movement: Classic sweep | Final game: patterns rotate by stage. Watch smooth movement, aimed shots and acceleration as enemies fall. |
| 19 | Enemy movement: Wave bounce | Final game: patterns rotate by stage. Watch smooth movement, aimed shots and acceleration as enemies fall. |
| 20 | Enemy movement: Diagonal bounce | Final game: patterns rotate by stage. Watch smooth movement, aimed shots and acceleration as enemies fall. |
| 21 | Enemy projectiles: 2 lives | Final adaptive difficulty: newly fired shots use speed 4 (normal). |
| 22 | Enemy projectiles: 1 life | Final adaptive difficulty: newly fired shots use speed 2 (reduced). |
| 23 | Falling obstacle: movement penalty | Real red obstacle staged at 8s. Catch it: movement halves for 2s. A shield blocks the slow. |
| 24 | Energy Shield | Best French: Heavy Tank only: staged shield pickup at 8s, protecting you for 15s. |
| 25 | Twin Cannon | Best French: staged activation at 8s; shoot to try both cannons. |
| 26 | Hyperfire | Best French: staged pickup at 8s; hold your fire key to compare fire rates. |
| 27 | Chameleon Hull | Best French: transformation at 8s; use Up/Down and touch enemies. |
| 28 | Cluster Grenade | Best French: staged pickup at 8s; press G to launch (if G is unbound). |
| 29 | Boss A: 850 HP, slow | Final boss mechanics and music; staged phase changes at 12/20s. Defeat it for real score, rewards, VFX and sound. |
| 30 | Boss B: 800 HP, fast | Final boss mechanics and music; staged phase changes at 12/20s. Defeat it for real score, rewards, VFX and sound. |
| 31 | Orbital Annihilator: stage 7 | Best French: staged pickup at 8s; hold Space to charge the laser. |
| 32 | Endless survival | Octopus / KimchiBaguette: zigzags and rising difficulty. Next ends this run. |
| 33 | Mission results | Review your demo score, lives, accuracy and unlocked achievements. |
| 34 | Standard Game Over: no-fire run | Do not shoot. Prepared lethal hit at 8s; full high-score table prevents name entry. Check Stage 2 Failed and replay. |
| 35 | Stage clear and progression | Prepared one-enemy stage: defeat it while alive; the real campaign advances to stage 2. |
| 36 | Campaign victory | Prepared final encounter: one enemy, boss with 50 HP. Clear both alive to show Game Cleared! |
| 37 | Life, shield and slow events | Heavy Tank: hit at 8s, life at 12s, shield at 16s, obstacle at 20s. Shield blocks the obstacle; watch real events. |
| 38 | Status event bus | Actual life, shield and slow events from the final game are listed here after gameplay. |
| 39 | Records and achievements | Chinese can fly: Left/Right switches tabs. This demo uses a separate profile. |
| 40 | High-score medals and icons | The Diversity Hires: rank medals, hearts, stars, settings and title icons. |
| 41 | Permanent upgrades and ship unlocks | Final catalog: Hull, Engine and Reloader have three tiers. Speedster costs 200 Credits. Purchases persist. |
| 42 | Credits, pending rewards and save retry | Real campaign and endless reward services. Optional buttons prepare a one-shot purchase save failure. |
| 43 | Purchased upgrades in campaign | Start a fresh campaign: bought Hull, Engine and Reloader upgrades affect lives, movement and firing. Endless is unchanged. |
| 44 | Custom levels: JSON import and export | Octopus: load and validate the bundled example, export a copy to the demo profile, then click Next to play it. |
| 45 | Custom level: two authored waves | Play the imported enemy grid and both waves. Custom play does not earn official credits, records or campaign unlocks. |
| 46 | Save, reload and restart | Reload real saved data here. Relaunch with the same demo profile to verify persistence. |
| 47 | Final build and presentation notes | The demo now uses main's final gameplay. Only scene preparation and navigation are presentation-specific. |
| 48 | End of the demonstration | Ready for questions. Use Back to revisit a scene, or Finish to close the demo. |

## Changes from the earlier demo

- Uses production ShopProducts, CampaignRewards and EndlessRewards; final credit HUD is live.
- Permanent Hull/Engine/Reloader tiers and Speedster unlock replace sample consumable stock.
- New campaign runs use purchased upgrade snapshots. Prepared ship comparisons intentionally show base ship behavior.
- Stage patterns, adaptive projectiles, red falling obstacles, slow events, boss defeat effects and audio use final production code.
- Standard now has 4 lives; Speedster 3; Heavy Tank 5. Only Heavy Tank accepts shields. Boss A has 850 HP.
- JSON panel uses JsonLevelReader, LevelGameplayAdapter and JsonLevelWriter. The next scene plays actual authored grids and waves.
- Custom gameplay does not submit official rewards, records or campaign progression.

## Remaining presentation limits

Main has no dedicated JSON import/export menu; this demo exposes the implemented APIs through its own panel.
The final production shop contains permanent upgrades and ships, not consumable shields.
No remote pending PR status is inferred. The previous missing-integration checklist has been superseded.

## Verification

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-demo.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\invoke-gradle.ps1 -Tasks check
```

The demo script runs navigation, cues, final catalog transactions, save retry, separate-JVM persistence,
JSON roundtrip and custom-record isolation tests, followed by all 48 scenes in accelerated headless mode.
