# Region Components Architecture

## Package Overview

The `org.aincraft.commands.components.region` package contains 7 focused components for managing guild regions.

## Component Diagram

```
RegionComponent (Router)
    ├─> RegionSelectionComponent (Creation workflow)
    ├─> RegionBasicComponent (CRUD & Display)
    ├─> RegionTypeComponent (Type management)
    ├─> RegionOwnerComponent (Owner management)
    ├─> RegionPermissionComponent (Permissions & Roles)
    └─> RegionCommandHelper (Shared utilities)
```

## Command Flow

### Example: `/g region settype myregion farm`

```
GuildsPlugin.registerGuildCommands()
    └─> RegionComponent.execute()
        └─> "settype" -> typeComponent.handleSetType(player, args)
            ├─> helper.requireGuild() -> gets player's guild
            ├─> helper.requireRegion() -> gets region by name
            ├─> helper.validateRegionType() -> validates type exists
            ├─> subregionService.setSubregionType() -> updates DB
            └─> player.sendMessage() -> confirms success
```

### Example: `/g region role create myregion farmer 3`

```
RegionComponent.execute()
    └─> "role" -> permissionComponent.handleRole()
        └─> "create" -> handleRoleCreate()
            ├─> helper.requireGuild()
            ├─> helper.requireRegion()
            ├─> helper.requireModifyPermission()
            ├─> permissionService.createRegionRole()
            └─> player.sendMessage() -> confirms role created
```

## Dependency Injection

### Constructor Dependencies by Component

**RegionCommandHelper**
- GuildMemberService
- SubregionService
- RegionPermissionService
- SubregionTypeRegistry

**RegionSelectionComponent**
- SelectionManager
- SubregionService
- PermissionService
- SubregionTypeRegistry
- GuildMemberService
- RegionCommandHelper

**RegionBasicComponent**
- SubregionService
- RegionVisualizer
- SubregionTypeRegistry
- RegionCommandHelper

**RegionTypeComponent**
- SubregionService
- SubregionTypeRegistry
- RegionTypeLimitRepository
- RegionCommandHelper

**RegionOwnerComponent**
- SubregionService
- RegionCommandHelper

**RegionPermissionComponent**
- SubregionService
- RegionPermissionService
- RegionCommandHelper

**RegionComponent (Router)**
- RegionSelectionComponent
- RegionBasicComponent
- RegionTypeComponent
- RegionOwnerComponent
- RegionPermissionComponent
- GuildMemberService

## Error Handling Pattern

All components use consistent error handling:

```java
// Validate prerequisites
Guild guild = helper.requireGuild(player);
if (guild == null) {
    return true;  // Error already sent via helper
}

// Check permissions
if (!helper.requireModifyPermission(region, player.getUniqueId(), player)) {
    return true;  // Error already sent
}

// Try operation
if (subregionService.deleteRegion(...)) {
    player.sendMessage(MessageFormatter.deserialize("<green>Success!</green>"));
} else {
    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Failed to delete region"));
}
```

## Message Formatting

All components use MessageFormatter for consistency:

```java
// Errors (red)
player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "You are not in a guild"));

// Warnings (yellow)
player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING, "No regions found"));

// Info display
player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Owner", "alice"));

// Usage/header
player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Region List", ""));
player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region create <name>", "Create new region"));

// MiniMessage format
player.sendMessage(MessageFormatter.deserialize("<green>Region created!</green>"));
```

## Adding New Features

### Scenario 1: Add new region option (e.g., password protection)

1. Add handler to appropriate component:
   - If it's metadata: RegionBasicComponent
   - If it's type-related: RegionTypeComponent
   - If it's security-related: RegionPermissionComponent

2. Add subcommand to RegionComponent router:
   ```java
   case "setpassword" -> basicComponent.handleSetPassword(player, args);
   ```

3. Implement handler following existing patterns

### Scenario 2: Add new role subcommand

1. Add method to RegionPermissionComponent:
   ```java
   private boolean handleRoleInherit(Player player, Guild guild, String[] args) {
       // Implementation
   }
   ```

2. Route in `handleRole()` method:
   ```java
   case "inherit" -> handleRoleInherit(player, guild, args);
   ```

### Scenario 3: Add region visualization feature

1. Enhance RegionBasicComponent.handleVisualize()
2. No changes needed to other components
3. Uses existing RegionVisualizer dependency

## Testing Approach

### Unit Test Examples

```java
// Test RegionCommandHelper validation
@Test
void testRequireGuild_ReturnsNull_WhenPlayerNotInGuild() {
    when(memberService.getPlayerGuild(any())).thenReturn(null);
    assertNull(helper.requireGuild(player));
    verify(player).sendMessage(contains("not in a guild"));
}

// Test RegionSelectionComponent in isolation
@Test
void testHandleCreate_ValidatesUniqueName() {
    helper.requireGuild(); // Mocked
    helper.validateRegionType(); // Mocked
    selectionComponent.handleCreate(player, args);
    verify(subregionService).getSubregionByName(...);
}

// Test RegionPermissionComponent
@Test
void testHandleSetPerm_RequiresModifyPermission() {
    when(helper.requireModifyPermission(...)).thenReturn(false);
    assertTrue(permissionComponent.handleSetPerm(player, args));
    verify(player).sendMessage(contains("permission"));
}
```

### Integration Test Examples

```java
// Test complete creation workflow
@Test
void testRegionCreationWorkflow() {
    regionComponent.execute(player, new String[]{"g", "create", "farm"});
    regionComponent.execute(player, new String[]{"g", "pos1"});
    regionComponent.execute(player, new String[]{"g", "pos2"});
    verify(subregionService).createSubregion(...);
}
```

## Performance Considerations

### Caching
- SubregionTypeRegistry caches type lookups
- No caching in RegionCommandHelper (lightweight operations)
- SubregionService handles database caching

### Scalability
- Helper methods are O(1) operations (simple validation)
- Component dispatch is O(1) via switch expression
- Database operations scale with repository implementation

### Thread Safety
- All components are stateless (singletons safe)
- All user data comes from request parameters
- SubregionService handles concurrency

## Maintenance Guidelines

### When to Add Code to RegionCommandHelper
- Validation logic used by 2+ components
- Error message formatting
- Type conversions

### When to Create New Component
- New subcommand category (5+ commands)
- Distinct business logic area
- No shared state with existing components

### When to Add to Existing Component
- Single command or closely related commands
- Shares most dependencies with existing methods
- Logically grouped (e.g., role.assign and role.unassign)

## Common Patterns

### Validation Chain
```java
Guild guild = helper.requireGuild(player);
if (guild == null) return true;

Subregion region = helper.requireRegion(guild, name, player);
if (region == null) return true;

if (!helper.requireModifyPermission(region, player.getUniqueId(), player)) return true;
```

### Service Call Pattern
```java
if (subregionService.deleteSubregion(guild.getId(), player.getUniqueId(), name)) {
    player.sendMessage(MessageFormatter.deserialize("<green>Deleted!</green>"));
} else {
    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Failed to delete"));
}
return true;
```

### Optional Handling
```java
Optional<Subregion> regionOpt = subregionService.getSubregionByName(guild.getId(), name);
if (regionOpt.isEmpty()) {
    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Not found"));
    return true;
}
Subregion region = regionOpt.get();
```
