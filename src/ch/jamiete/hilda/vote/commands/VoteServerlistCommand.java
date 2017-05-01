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
package ch.jamiete.hilda.vote.commands;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.vote.Vote;
import ch.jamiete.hilda.vote.VotePlugin;
import ch.jamiete.hilda.vote.VoteResponse;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

public class VoteServerlistCommand extends ChannelSubCommand {
    private final VotePlugin plugin;

    protected VoteServerlistCommand(final Hilda hilda, final ChannelSeniorCommand senior, final VotePlugin plugin) {
        super(hilda, senior);

        this.plugin = plugin;

        this.setName("serverlist");
        this.setDescription("Lists all the votes currently open on the server.");
        this.setMinimumPermission(Permission.MANAGE_SERVER);
    }

    @Override
    public void execute(final Message message, final String[] arguments, final String label) {
        final ArrayList<Vote> eligible = new ArrayList<Vote>();
        for (final Vote vote : this.plugin.getVotes()) {
            if (this.hilda.getBot().getTextChannelById(vote.getChannelId()).getGuild() == message.getGuild()) {
                eligible.add(vote);
            }
        }

        if (eligible.size() == 0) {
            this.reply(message, "I am not managing any votes right now.");
            return;
        }

        final EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Currently managing " + eligible.size() + " " + (eligible.size() + eligible.size() == 1 ? "vote" : "votes"), null);

        for (final Vote vote : eligible) {
            final MessageBuilder mb = new MessageBuilder();

            mb.append("ID ").append(vote.getId(), Formatting.ITALICS).append("\n");
            mb.append("Opened in ").append(this.hilda.getBot().getTextChannelById(vote.getChannelId()).getName(), Formatting.ITALICS);
            mb.append(" by ").append(vote.getOpener(), Formatting.ITALICS).append("\n");
            mb.append("Received ").append(vote.getResponses().size()).append(" ").append(vote.getResponses().size() == 1 ? "response" : "responses");

            if (!vote.getResponses().isEmpty()) {
                final int yea = (int) vote.getResponses().values().stream().filter(response -> response == VoteResponse.YEA).count();
                final int nay = (int) vote.getResponses().values().stream().filter(response -> response == VoteResponse.NAY).count();

                mb.append("\n").append("Currently at ");

                if (nay == 0) {
                    mb.append("100");
                } else if (yea == 0) {
                    mb.append("0");
                } else {
                    mb.append(Math.round((double) yea / (yea + nay) * 100));
                }

                mb.append("% acceptance");
            }

            eb.addField(StringUtils.abbreviate(vote.getQuestion(), 256), mb.build().getContent(), false);
        }

        this.reply(message, eb.build());
    }

}
