# CustomRaids

A Bukkit plugin that work as a plugin for [CustomEvents](https://github.com/Flowsqy/CustomEvents). It spawns custom
entities and create a chest with custom rewards when every entity is killed.

## CustomEvents configuration

The type of the event is `raids`

The event section :

```yaml
event:
  world:
    name: <world-name>
    alias: <world-alias>
    radius: <world-radius>
  entities:
    <entity-1>: <entity>
    <entity-2>: <entity>
    ...
  rewards:
    <item-1>: <item>
    <item-2>: <item>
    ...
  messages:
    start: <message-start>
    end: <message-end>

# Where:
#
# World:
# <world-name> [String] : The world name (which is also the world folder name) where the event should take place
# <world-alias> [String] : The name of the world. It supports colors.
# <world-radius> [integer] : The radius of the circle where the event should take place.
#   Its center is at the coordinates (0;0). Every position contained in this circle is valid and chosen randomly.
#
# Entities:
# <entity-n> : The key of an entity. You can have as many entities as you want. The key just need to be unique.
# <entity> [https://github.com/Flowsqy/AbstractMob] :
#   An AbstractMob entity. Represent an entity that will be spawned.
#
# Rewards:
# <item-n> : The key of a reward. You can have as many entities as you want. The key just need to be unique.
# <item> [https://github.com/Flowsqy/AbstractMenu] (Inventory section) :
#   Representing a reward item. 
#   This section is deserialized like a AbstractMenu inventory.
#   As the items will be in a chest, title and line are ignored.
#
# Messages:
# <message-start> [String] : The start message. It supports colors.
#   %x% and %z% will be replaced with the x and y coordinates of the event.
#   %world% will be replaced with the world name given above.
# <message-end> [String] : The end message. It supports colors.
#   %x% and %z% will be replaced with the x and y coordinates of the event.
#   %world% will be replaced with the world name given above.
```

For example:

```yaml
date:
  1:
    day: 1
    hour: 18
    minute: 0

type: raids

data:
  command-id: "easy-raid"

event:
  world:
    name: world
    alias: "overworld"
    radius: 300
  entities:
    1:
      type: ZOMBIE
      quantity: 5
      radius: 4
      living:
        keep-when-far-away: true
    2:
      type: SKELETON
      quantity: 2
      radius: 4
      living:
        keep-when-far-away: true
    3:
      type: CREEPER
      quantity: 1
      radius: 4
      living:
        keep-when-far-away: true
      creeper:
        charged: true
  rewards:
    1:
      slots:
        - 13
      item:
        type: APPLE
        amount: 1
  messages:
    start: "&6The raid starts at &a%x% %z% &6in the &a%world%"
    end: "&6The raid ends at &a%x% %z% &6in the &a%world%&6. You can collect your rewards"
```

## Building

Just clone the repository and do `mvn clean install` or `mvn clean package`. The jar is in the _target_ directory.