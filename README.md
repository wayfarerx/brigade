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

 - [Initial Setup](#InitialSetup)
   - [Inviting and Configuring](InvitingandConfiguring)
   - [Specifying Channels](SpecifyingChannels)
 - [Managing Team Rosters](ManagingTeamRosters)
   - [Opening a Roster](OpeningaRoster)
   - [Aborting an Event](AbortinganEvent)
   - [Closing a Roster](ClosingaRoster)

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

`!event @administrator*`

The pinned message must contain the word `!event` followed by mentions
of zero-or-more users that are designated administrators in this
channel. The server owner is always considered an administrator whether
or not any other users are designated.

There may be any number of pinned `!event` messages and the server owner
is free to edit, unpin or delete them at any time. The bot will be
updated with the new administrator list immediately.

## Managing Team Rosters

Event administrators can control the lifecycle of a roster that is used
to build a team.

### Opening a Roster

### Aborting an Event

### Closing a Roster