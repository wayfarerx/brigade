# circumvolve

Circumvolve is a Discord bot that tries to assist in building
optimal-but-fair teams on a repeating basis. The basic idea is that
*administrators* set up a roster specifying what *roles* they require
and how many *members* they require for each role. For example: a
traditional MMO dungeon group would look something like:

 - 1 Tank
 - 1 Healer
 - 2 Damage dealers
 
Circumvolve enables administrators to ask for these roles to be filled
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

## Initial Setup

Setting up your Discord server to use circumvolve consists of inviting
the bot, configuring your server's permissions and specifying which
channels the bot can communicate on.

### Inviting and Configuring

Currently, the only way to invite circumvolve to your server is to
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

When you first open an event, Circumvolve will post a message containing
the initial team load out. As the users that have volunteered or been
assigned to the team change Circumvolve will update that message with
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
of those roles. Circumvolve remembers the order that you specify what
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
forget exactly what you are volunteered for. There is a simple command
that takes an optional user mention that will remind you of where things
stand:

`!? @USER?`

The bot will respond with the list of roles the specified user is
volunteered for in order of how they've expressed their preferences.

So `!? @wayfarerx` will show you how wayfarer has volunteered, while
`!?` will show what YOU have volunteered for.