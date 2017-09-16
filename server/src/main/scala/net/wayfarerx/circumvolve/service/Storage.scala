/*
 * Storage.scala
 *
 * Copyright 2017 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wayfarerx.circumvolve.service

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions

import collection.JavaConverters._
import com.amazonaws.services.s3.model.ListObjectsRequest
import net.wayfarerx.circumvolve.model.{Roster, Team}

/**
 * Base class for event storage strategies.
 */
trait Storage {

  /**
   * Loads the roster for a specified guild and channel.
   *
   * @param guildId   The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @return The roster for a specified guild and channel.
   */
  def getRoster(guildId: String, channelId: String): Option[Roster]

  /**
   * Saves the roster for a specified guild and channel.
   *
   * @param guildId   The ID of the guild to save for.
   * @param channelId The ID of the channel to save for.
   * @param roster    The roster to save.
   */
  def putRoster(guildId: String, channelId: String, roster: Roster): Unit

  /**
   * Deletes the roster for a specified guild and channel.
   *
   * @param guildId   The ID of the guild to delete for.
   * @param channelId The ID of the channel to delete for.
   */
  def deleteRoster(guildId: String, channelId: String): Unit

  /**
   * Lists the specified number of the most recently saved teams for a guild and channel.
   *
   * @param guildId   The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @param count     The number of teams to load.
   * @return The requested collection of team IDs.
   */
  def listTeamIds(guildId: String, channelId: String, count: Int): Vector[String]

  /**
   * Loads the specified team information.
   *
   * @param guildId   The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @param teamId    The ID of the team to load.
   * @return The specified team information.
   */
  def getTeam(guildId: String, channelId: String, teamId: String): Option[Team]

  /**
   * Saves the specified team information.
   *
   * @param guildId   The ID of the guild to save for.
   * @param channelId The ID of the channel to save for.
   * @param teamId    The ID of the team to save.
   * @param team      The team information to save.
   */
  def putTeam(guildId: String, channelId: String, teamId: String, team: Team): Unit

}

/**
 * Implementation of the storage interface.
 */
object Storage {

  /**
   * A dummy storage instance that is always empty and never saves anything.
   */
  object Empty extends Storage {

    /* Always return none. */
    override def getRoster(guildId: String, channelId: String): Option[Roster] = None

    /* Do nothing. */
    override def putRoster(guildId: String, channelId: String, roster: Roster): Unit = ()

    /* Do nothing. */
    override def deleteRoster(guildId: String, channelId: String): Unit = ()

    /* Always return empty. */
    override def listTeamIds(guildId: String, channelId: String, count: Int): Vector[String] = Vector()

    /* Always return none. */
    override def getTeam(guildId: String, channelId: String, teamId: String): Option[Team] = None

    /* Do nothing. */
    override def putTeam(guildId: String, channelId: String, teamId: String, team: Team): Unit = ()

  }

  /**
   * A storage implementation that loads and saves using AWS S3.
   */
  final class S3Storage(bucket: String, path: String, accessKey: String, secretKey: String) extends Storage {

    /** The S3 client to use. */
    private val s3 = com.amazonaws.services.s3.AmazonS3ClientBuilder.standard.withRegion(Regions.US_WEST_2)
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey))).build()

    /* Load a roster from '<bucket>/<path>/<guildId>/<channelId>/roster.json'. */
    override def getRoster(guildId: String, channelId: String): Option[Roster] =
      read(s"$path/$guildId/$channelId/roster.json") map Roster.read

    /* Save the roster to '<bucket>/<path>/<guildId>/<channelId>/roster.json'. */
    override def putRoster(guildId: String, channelId: String, roster: Roster): Unit =
      write(s"$path/$guildId/$channelId/roster.json", roster.write())

    /* Delete the roster at '<bucket>/<path>/<guildId>/<channelId>/roster.json'. */
    override def deleteRoster(guildId: String, channelId: String): Unit =
      delete(s"$path/$guildId/$channelId/roster.json")

    /* Return the names of the first <count> objects in '<bucket>/<path>/<guildId>/<channelId>/teams/'. */
    override def listTeamIds(guildId: String, channelId: String, count: Int): Vector[String] =
      list(s"$path/$guildId/$channelId/teams/", count)

    /* Load a team from '<bucket>/<path>/<guildId>/<channelId>/teams/<teamId>.json'. */
    override def getTeam(guildId: String, channelId: String, teamId: String): Option[Team] =
      read(s"$path/$guildId/$channelId/teams/$teamId.json") map Team.read

    /* Save the team to '<bucket>/<path>/<guildId>/<channelId>/teams/<teamId>.json'. */
    override def putTeam(guildId: String, channelId: String, teamId: String, team: Team): Unit =
      write(s"$path/$guildId/$channelId/teams/$teamId.json", team.write())

    /**
     * Reads the entire content of an S3 object as a string.
     *
     * @param key The path to the S3 object.
     * @return The entire content of an S3 object as a string.
     */
    private def read(key: String): Option[String] =
      if (!s3.doesObjectExist(bucket, key)) None else {
        try {
          Some(s3.getObjectAsString(bucket, key))
        } catch {
          case e: Exception =>
            e.printStackTrace() // TODO log
            None
        }
      }

    /**
     * Writes the entire content of a string to an S3 object.
     *
     * @param key  The path to the S3 object.
     * @param data The data to write to the S3 object.
     */
    private def write(key: String, data: String): Unit =
      try {
        s3.putObject(bucket, key, data)
      } catch {
        case e: Exception =>
          e.printStackTrace() // TODO log
      }

    /**
     * Deletes an S3 object.
     *
     * @param key The path to the S3 object.
     */
    private def delete(key: String): Unit =
      try {
        s3.deleteObject(bucket, key)
      } catch {
        case e: Exception =>
          e.printStackTrace() // TODO log
      }

    /**
     * Lists the keys under the specified prefix.
     *
     * @param prefix The prefix to list keys under.
     * @param count  The maximum number of keys to list.
     * @return The keys under the specified prefix.
     */
    private def list(prefix: String, count: Int): Vector[String] =
      try {
        s3.listObjects(new ListObjectsRequest().withBucketName(bucket).withPrefix(prefix).withMaxKeys(count))
          .getObjectSummaries.asScala.map(_.getKey.substring(prefix.length)).toVector
      } catch {
        case e: Exception =>
          e.printStackTrace() // TODO log
          Vector()
      }

  }


}