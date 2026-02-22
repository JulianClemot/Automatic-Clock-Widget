# Gitmoji Commit

Create a git commit following the [gitmoji](https://gitmoji.dev) convention.

## Commit Format

```
<emoji> <description>

[optional body]
```

- **Emoji**: A single gitmoji representing the type of change
- **Description**: Short imperative sentence (no period, max ~72 chars total)
- **Body** (optional): More context if the change is complex

## Gitmoji Reference

| Emoji | Code | Use when... |
|-------|------|-------------|
| ✨ | `:sparkles:` | Introducing a new feature |
| 🐛 | `:bug:` | Fixing a bug |
| ♻️ | `:recycle:` | Refactoring code |
| 🎨 | `:art:` | Improving structure or format |
| ⚡️ | `:zap:` | Improving performance |
| 🔥 | `:fire:` | Removing code or files |
| 📝 | `:memo:` | Adding or updating documentation |
| ✅ | `:white_check_mark:` | Adding or updating tests |
| 🧪 | `:test_tube:` | Adding a failing test |
| 🚧 | `:construction:` | Work in progress |
| 💄 | `:lipstick:` | Updating UI or styles |
| 🏗️ | `:building_construction:` | Making architectural changes |
| 🔧 | `:wrench:` | Adding or updating configuration |
| ⬆️ | `:arrow_up:` | Upgrading dependencies |
| ⬇️ | `:arrow_down:` | Downgrading dependencies |
| ➕ | `:heavy_plus_sign:` | Adding a dependency |
| ➖ | `:heavy_minus_sign:` | Removing a dependency |
| 🚚 | `:truck:` | Moving or renaming files |
| 🔀 | `:twisted_rightwards_arrows:` | Merging branches |
| ⏪️ | `:rewind:` | Reverting changes |
| 💥 | `:boom:` | Introducing breaking changes |
| 🔒️ | `:lock:` | Fixing security issues |
| 🚑️ | `:ambulance:` | Critical hotfix |
| 🎉 | `:tada:` | Beginning a project |
| 🔖 | `:bookmark:` | Releasing a version |
| 💡 | `:bulb:` | Adding or updating comments |
| 🗑️ | `:wastebasket:` | Deprecating code |
| ⚰️ | `:coffin:` | Removing dead code |
| 🩹 | `:adhesive_bandage:` | Simple fix for a non-critical issue |
| 🧱 | `:bricks:` | Infrastructure changes |
| 👔 | `:necktie:` | Adding or updating business logic |
| 🔊 | `:loud_sound:` | Adding or updating logs |
| 🔇 | `:mute:` | Removing logs |

## Steps

1. Run `git diff` and `git status` to understand what changed
2. Pick the **single most representative** gitmoji for the change
3. Write a concise, imperative description (e.g. "add URL validation" not "added URL validation")
4. Stage relevant files and create the commit

## Examples

```
✨ add timezone auto-detection from calendar events
🐛 fix widget not updating after airplane mode toggled
♻️ extract clock formatting logic into ClockDisplayFormatter
✅ add unit tests for AddUrlUseCase deduplication
⬆️ upgrade Kotlin to 2.3.0 and AGP to 8.13.2
🔧 add airports.properties template to properties/
🔥 remove legacy SharedPreferences migration code
```

## Rules

- Use the emoji character directly (not the `:code:` form)
- One emoji per commit — if you need two, split the commit
- Description in English, lowercase, imperative mood
- No period at the end of the description
