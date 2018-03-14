# brigade

Brigade is a Discord bot that tries to assist in building
optimal-but-fair teams on a repeating basis. The basic idea is that
*administrators* set up a roster specifying what *roles* they require
and how many *members* they require for each role. For example: a
traditional MMO dungeon group would look something like:

 - 1 Tank
 - 1 Healer
 - 2 Damage dealers
 
Brigade enables administrators to ask for these roles to be filled
and allows members to volunteer to fill those roles, all without any
other direct human intervention.

 - [Initial Setup](#initial-setup)
   - [Inviting and Configuring](#inviting-and-configuring)
   - [Specifying Channels](#specifying-channels)
 - [Managing Team Rosters](#managing-team-rosters)
   - [Opening a Roster](#opening-a-roster)
   - [Aborting an Event](#aborting-an-event)
   - [Closing a Roster](#closing-a-roster)
 - [Volunteering For and Dropping From Events](#volunteering-for-and-dropping-from-events)
   - [Volunteering For Events](#volunteering-for-events)
   - [Dropping Out of Events](#dropping-out-of-events)
   - [Seeing What You Have Volunteered For](#seeing-what-you-have-volunteered-for)
 - [Other Administrative Commands](#other-administrative-commands)
   - [Assigning and Releasing Users](#assigning-and-releasing-users)
   - [Offering and Kicking Users](#offering-and-kicking-users)
 - [The Help Command](#the-help-command)
 - [The Team Builder](#the-team-builder)
 - [Upcoming Features](#upcoming-features)

## Initial Setup

Setting up your Discord server to use brigade consists of inviting
the bot, configuring your server's permissions and specifying which
channels the bot can communicate on.

### Inviting and Configuring

Currently, the only way to invite brigade to your server is to
contact wayfarerx, but don't do that. This is still alpha software and
if you don't already have access you're unlikely to get it.

Once invited, the bot is going to need certain permissions on the
channels you want him to use:

  - Read Messages
  - Send Messages

It is up to the server administrators to grant the bot a role that is
capable of performing those actions on the desired channels.

### Specifying Channels

Once the bot is set up the owner of the server needs to set up channels
that the bot should interact in. Each channel is capable of hosting a
single event and building exactly one team at a time. A channel is
specified as an event channel by pinning a message from the server owner
with the following command:

`!event @USER*`

The pinned message must contain the word `!event` followed by mentions
of zero-or-more users that are designated administrators in this
channel. The server owner is always considered an administrator whether
or not any other users are designated.

There may be any number of pinned event messages in a channel and the
server owner is free to edit, unpin or delete them at any time. The bot
will be updated with the new administrator list immediately.

Your event messages may also contain other arbitrary text around the
command. For example, you could also describe your channel in your
pinned message:

```
This channel is for signing up for our weekly dungeon run!
!event @Bob @Jan
```

## Managing Team Rosters

Event administrators can control the lifecycle of a roster that is used
to build a team. The birth, life and death of an event's roster is as
follows:

 - First, an administrator opens a roster, describes what roles they
   need and how many players are required for each role.
 - Users volunteer for whatever roles they are interested in, possibly
   changing their minds over time.
 - At some point in the future an administrator either aborts the event,
   declining to form a team or they close the roster and finalize a
   team.

The distinction between aborting an event and closing the roster is
significant. When a roster is closed a team is formed and that team
is saved for perpetuity. Saved teams may influence the creation of
future teams so they should be as accurate as possible. By contrast,
aborting an event disposes of the roster without affecting any future
teams.

### Opening a Roster

An administrator can open a roster using the following command:

`!open (!ROLE, COUNT)+`

The open command takes one or more pairs of role names along with a
number value that defines how many members are needed for that role.
Role names may be any single word and are not case sensitive but must
always be preceded by the `!` character. For example, to open a roster
for the traditional MMO dungeon group referenced above you could type:

`!open !tank 1 !healer 1 !dps 2`

You may call the open command again and again after initially opening a
roster: it will change the roles needed and the number of players
required for each role but will keep any assignments that have been made
and all the roles that users have already volunteered for.

Your open messages may also contain other arbitrary text around the
command. For example, you could also describe your event in your
open message:

```
Running some hard mode dungeons this Thursday at 7:00 pm eastern!
!open !tank 1 !healer 1 !dps 2
```

When you first open an event, Brigade will post a message containing
the initial team load out. As the users that have volunteered or been
assigned to the team change Brigade will update that message with
the new team load out.

### Aborting an Event

Aborting an event disposes of the roster as well as all assignments and
any roles that have been volunteered for. To abort an event issue a
simple command:

`!abort`

You may also put your abort command with other text describing why the
event was aborted:

```
Sorry everyone, we couldn't get enough people to join us tonight.
!abort
```

Aborting an event is important if you actually end up cancelling an
event: it prevents the bot from recording anyone's participation and
potentially affecting the composition of future teams.

### Closing a Roster

Closing a roster finalizes the team and prevents any more assignments or
volunteers from affecting the team makeup. To close a roster use the
command:

`!close`

You may also include other text with your close command:

```
Alright everyone let's do this!
!close
```

Closing a roster forms the final team and saves the makeup for that team
so that future teams built in this channel can look back at who has
recently filled what roles.

Once a roster is closed no more assignments or volunteers are honored
until a new roster has been opened.

## Volunteering For and Dropping From Events

Any user that can send messages to an event channel can volunteer for
one or more roles and can also change their minds later and drop out
of those roles. Brigade remembers the order that you specify what
roles you volunteer for and assumes that you volunteered for your most
desirable role first and any backup roles after that.

For example: if you initially volunteer for `healer` and `dps`, the bot
assumes you would most enjoy healing but would also be willing to be a
damage dealer. If you then later volunteer for `tank` it assumes that
tanking, while something you are willing to do, it is your least
desirable role.

Dropping roles works much the same way. Assuming you had volunteered as
above, if you subsequently dropped the `dps` role you would then be
considered to have volunteered for `healer` first and `tank` as a
backup.

### Volunteering For Events

To volunteer for an event you simply list one or more roles you wish to
fill:

`!ROLE+`


For example: to volunteer as a healer but also signal that you are
willing to be a damage dealer if needed you would type:

`!healer !dps`

If you wanted to volunteer as ONLY a tank you would type:

`!tank`

You are also able to mix other text with your role commands and split
up role commands over multiple messages:

`Bringing my awesome elven !healer witch.`

`Or my mediocre human thief for !dps if you need him.`

### Dropping Out of Events

Sometimes plans change and you have to step away from an event you have
previously volunteered for. Or perhaps you changed your mind and just
want to drop one of the multiple roles you volunteered for. This is what
the drop command is for:

`!drop !ROLE*`

For example, say that you had volunteered to be a healer or damage
dealer but later decided you were not in a healing mood you could type:

`!drop !healer`

Or maybe your computer caught on fire and you have to fully bow out of
the event:

`!drop`

Like other commands you can mix text in around your command to explain
your thinking:

`Sorry all I have to !drop !healer, I rebuilt my witch for PvP.`

Drop commands, when you're dropping only some of the roles you've
volunteered for, will *edit* the list of roles associated with you. This
means that the following sequence of commands:

`!tank !healer !dps`

`!drop !healer`

...is equivalent to the single command:

`!tank !dps`

### Seeing What You Have Volunteered For

In the hustle and bustle of building a large team over many days you may
forget exactly what you or someone else has volunteered for. There is a
simple command that takes an optional user mention that will remind you
of where things stand:

`!? @USER?`

The bot will respond with the list of roles the specified user is
volunteered for in order of how they've expressed their preferences.

So `!? @wayfarerx` will show you how wayfarer has volunteered, while
`!?` will show what YOU have volunteered for.

## Other Administrative Commands

Sometimes administrators need to step in and exert control over exactly
how a team gets built. The following commands give administrators
fine-grained control of the details that go into filling a roster.

### Assigning and Releasing Users

Administrators are allowed to override the team-building logic and
directly assign users to specific roles with the assign command:

`!assign (@USER !ROLE)+`

The assign command takes one or more pairs of user mentions and role
names and makes sure those role assignments are honored before roles
are filled with volunteers. So to assign your main tank and healer you
could say:

`!assign @Bob !tank @Sue !healer`

Much like when volunteering for a command, assignments are applied in
the order they are received. So if you want to specify the main and off
tanks you could type:

`!assign @Bob !tank @Sue !tank`

...which would make `@Bob` the main tank and `@Sue` the off tank.

With great power comes great responsibility and sometimes administrators
that assign users to roles must take those assignments back when the
situation changes. That's what the release command is for:

`!release @USER+`

The release command allows administrators to release one or more users
from previously assigned roles. So if you took the example above where
we assigned a tank and healer:

`!assign @Bob !tank @Sue !healer`

...but later found out `@Bob` couldn't make it, we could release the
tank role back to any volunteers.

`!release @Bob`

Like other commands, assign and release can be mixed in with other text:

`I'm going to !assign @Bob !tank because we need his big health pool.`

`I have to !release @Sue because her dog is having puppies.`

### Offering and Kicking Users

Administrators may also emulate volunteer and drop commands from other
users.

The offer command directly emulates other users volunteering for one or
more roles:

`!offer @USER !ROLE+`

So an administrator typing:

`!offer @Bob !tank !dps`

...would have the exact same effect as `@Bob` himself typing:

`!tank !dps`

This is mostly useful if a user is unable to connect to Discord but has
requested that an admin step in on their behalf.

Conversely, administrators can remove users from some or all roles with
the kick command:

`!kick @USER !ROLE*`

Similar to offer, kick emulates other users dropping one or more roles.
So an administrator typing:

`!kick @Bob !tank`

...would have the exact same effect as `@Bob` himself typing:

`!drop !tank`

The kick command is useful for when someone fails to show up for an
event and the administrator wants the team builder to exclude them. Say
`@Sue` fails to show up, the administrator can type:

`!kick @Sue`

...and the team builder will progress as if `@Sue` had never
volunteered.

As you have come to expect, offer and kick commands can be included with
other text:

`So he's at work but I'm supposed to !offer @Bob !tank while he's gone.`

`Since she's not online I'm going to !kick @Sue from the team.`

## The Help Command

The help command is a simple command that displays a short version of
this documentation in the Discord channel:

`!help`

The help command also prints a link that points back to this
documentation.

## The Team Builder

The team builder is a multi-step, heuristic algorithm that attempts to
build teams in a fair and equitable manner over multiple incarnations
of an event.

There are three major considerations that are taken into account when
building a team:

 - The preferences for roles expressed by volunteers.
 - The history of what users have filled what roles recently.
 - The order that users volunteered for specific roles.

The team builder works step-by-step through the above considerations,
adding one user to the team at a time using an approximation of the
following logic:

 - First, the builder processes all assignments, filling roles with
   users in the order they were assigned until no assignments are left.
 - Next, the builder selects a role with open slots to fill. It favors
   roles for which users have expressed a preference for, falling back
   to roles that still need the most members.
 - After selecting a role, a user is selected to fill this role by:
   - Finding users with the highest preference for the role.
   - Sorting those users with a scoring algorithm based on how recently
     they have filled that role, favoring users who have not filled that
     role as recently.
   - Breaking any ties by picking users that signed up first.
 - Finally, the builder will loop back, select another role with
   unfilled slots and proceeding as above until the team is full or no
   volunteers remain.

Currently, the historical scoring logic is fairly basic. Future versions
of this bot might allow for more customization in this regard.

## Upcoming Features

As cool as Brigade is, it could always do more:

 - [Allowing guild owners or administrators to influence the historical
   scoring algorithm on a per-channel basis.](https://github.com/wayfarerx/brigade/issues/3)
 - [Supporting the construction of multiple teams in the case that there
   are enough volunteers to do so.](https://github.com/wayfarerx/brigade/issues/4)
 - Anything else you can think of! Just open an issue
   [here on GitHub](https://github.com/wayfarerx/brigade/issues/new)
   or hit me up on Discord: @wayfarerx.
