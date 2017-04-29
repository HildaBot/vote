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

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelCommand;
import ch.jamiete.hilda.vote.Vote;
import ch.jamiete.hilda.vote.VotePlugin;
import ch.jamiete.hilda.vote.VoteResponse;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class VoteRegisterCommand extends ChannelCommand {
    private final VotePlugin plugin;

    protected VoteRegisterCommand(final Hilda hilda, final VotePlugin plugin) {
        super(hilda);

        this.plugin = plugin;

        this.setName("register");
        this.setDescription("Registers a vote.");
        this.setAliases(Arrays.asList(new String[] { "r" }));
    }

    @Override
    public void execute(final Message message, final String[] arguments, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        final Vote vote = this.plugin.getVoteByID(arguments[0]);

        if (vote == null || vote != null && !this.hilda.getBot().getTextChannelById(vote.getChannelId()).equals(message.getChannel())) {
            this.reply(message, "I couldn't find that vote.");
            return;
        }

        VoteResponse response = null;

        try {
            response = VoteResponse.valueOf(arguments[1].toUpperCase());
        } catch (final IllegalArgumentException e) {
        }

        if (response == null) {
            this.reply(message, "I don't recognise that response to the vote. You must say 'yea', 'nay' or 'abstain'.");
            return;
        }

        if (vote.hasVoted(member)) {
            final MessageBuilder mb = new MessageBuilder();

            if (vote.getResponses().get(member.getUser().getId()) == response) {
                this.reply(message, "You've already voted for that option.");
            } else {
                mb.append("Okay ").append(member.getAsMention()).append(", I've changed your vote from ");
                mb.append("*").append(vote.getResponses().get(member.getUser().getId()).toString().toLowerCase()).append("* ");
                mb.append("to *").append(response.toString().toLowerCase()).append("*.");

                vote.getResponses().remove(member.getUser().getId());
                vote.getResponses().put(member.getUser().getId(), response);

                this.reply(message, mb.build());
            }
        } else {
            vote.getResponses().put(member.getUser().getId(), response);
            this.reply(message, "Okay " + member.getAsMention() + ", I've set your vote to *" + response.toString().toLowerCase() + "*.");
        }

        vote.check();
    }

}
