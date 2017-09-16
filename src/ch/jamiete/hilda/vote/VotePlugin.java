/*******************************************************************************
 * Copyright 2017 jamietech
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
 *******************************************************************************/
package ch.jamiete.hilda.vote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.plugins.HildaPlugin;
import ch.jamiete.hilda.vote.commands.VoteBaseCommand;

public class VotePlugin extends HildaPlugin {
    public static final long MAXIMUM_LENGTH = 129600000; // 36 hours

    private final ArrayList<Vote> votes = new ArrayList<Vote>();

    public VotePlugin(final Hilda hilda) {
        super(hilda);
    }

    /**
     * Adds a vote to the manager.
     * @param vote The vote to add
     */
    public void addVote(final Vote vote) {
        this.votes.add(vote);
    }

    /**
     * Gets a unique ID that isn't currently registered.
     * @return A unique ID
     */
    public String getFreshID() {
        Hilda.getLogger().fine("Generating vote ID...");

        final String alphabet = "abcdefghijkmnpqrstuvwxyz";
        final String numbers = "23456789";
        final Random random = new Random();

        final String possibleid = String.valueOf(alphabet.charAt(random.nextInt(alphabet.length()))) + String.valueOf(numbers.charAt(random.nextInt(numbers.length())));

        for (final Vote vote : this.votes) {
            if (vote.getId().equals(possibleid)) {
                return this.getFreshID();
            }
        }

        Hilda.getLogger().fine("Found an ID: " + possibleid);

        return possibleid;
    }

    /**
     * Get the vote with that ID.
     * @param id The ID to test
     * @return The vote with that ID or null if none exists
     */
    public Vote getVoteByID(final String id) {
        return this.votes.stream().filter(v -> v.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    /**
     * Gets a list of the votes managed by this command.
     * @return A list of votes
     */
    public List<Vote> getVotes() {
        return Collections.unmodifiableList(this.votes);
    }

    @Override
    public void onDisable() {
        this.save();
    }

    @Override
    public void save() {
        final File folder = new File("data");

        if (!folder.isDirectory()) {
            folder.mkdir();
        }

        try {
            final File file = new File(folder, "votes.hilda");

            if (!file.exists()) {
                file.createNewFile();
            }

            final FileOutputStream stream = new FileOutputStream("data/votes.hilda", false);
            final ObjectOutputStream obj = new ObjectOutputStream(stream);

            obj.writeObject(this.votes);

            Hilda.getLogger().fine("Saved " + this.votes.size() + " votes to disk");

            obj.close();
            stream.close();
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.SEVERE, "Failed to save votes to disk", e);
        }
    }

    @Override
    public void onEnable() {
        this.getHilda().getCommandManager().registerChannelCommand(new VoteBaseCommand(this.getHilda(), this));

        final File file = new File("data/votes.hilda");

        if (!file.exists()) {
            Hilda.getLogger().info("No votes saved to disk");
            return;
        }

        final ArrayList<Vote> loaded = new ArrayList<Vote>();
        int ended = 0;
        int rejected = 0;

        try {
            final FileInputStream stream = new FileInputStream(file);
            final ObjectInputStream obj = new ObjectInputStream(stream);

            @SuppressWarnings("unchecked")
            final ArrayList<Vote> list = (ArrayList<Vote>) obj.readObject();

            if (list != null && !list.isEmpty()) {
                loaded.addAll(list);
            }

            obj.close();
            stream.close();

            file.delete();
        } catch (final Exception e) {
            Hilda.getLogger().log(Level.SEVERE, "Failed to load votes from disk", e);
        }

        for (final Vote vote : loaded) {
            vote.setHilda(this.getHilda());
            vote.setPlugin(this);

            if (this.getHilda().getBot().getTextChannelById(vote.getChannelId()) == null) {
                rejected++;
                continue;
            }

            if (vote.getCommencement() == Long.MIN_VALUE) {
                vote.setCommencement(System.currentTimeMillis());
            }

            for (final Entry<String, VoteResponse> response : vote.getResponses().entrySet()) {
                if (this.getHilda().getBot().getUserById(response.getKey()) == null) {
                    vote.getResponses().remove(response.getKey());
                }
            }

            if (System.currentTimeMillis() >= vote.getCommencement() + VotePlugin.MAXIMUM_LENGTH) {
                vote.finish();
                ended++;
                continue;
            } else {
                final long remaining = vote.getCommencement() + VotePlugin.MAXIMUM_LENGTH - System.currentTimeMillis();
                final ScheduledFuture<?> future = this.getHilda().getExecutor().schedule(new VoteTimer(vote), remaining, TimeUnit.MILLISECONDS);
                vote.setFuture(future);
            }

            this.votes.add(vote);

            vote.check();
        }

        Hilda.getLogger().info("Loaded " + this.votes.size() + " current votes from disk");

        if (ended > 0) {
            Hilda.getLogger().info("Loaded and ended " + ended + " expired votes from disk");
        }

        if (rejected > 0) {
            Hilda.getLogger().info("Loaded and rejected " + rejected + " malformed votes from disk");
        }
    }

    /**
     * Removes a vote from the manager.
     * @param vote The vote to remove
     */
    public void remove(final Vote vote) {
        this.votes.remove(vote);
    }

}
