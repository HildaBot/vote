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

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelCommand;
import ch.jamiete.hilda.vote.Vote;
import ch.jamiete.hilda.vote.VotePlugin;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class VoteDeleteCommand extends ChannelCommand {
    private final VotePlugin plugin;

    protected VoteDeleteCommand(final Hilda hilda, final VotePlugin plugin) {
        super(hilda);

        this.plugin = plugin;

        this.setName("delete");
        this.setDescription("Deletes a vote.");
    }

    @Override
    public void execute(final Message message, final String[] arguments, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (arguments.length != 1) {
            this.usage(message, "delete <id>", label);
            return;
        }

        final Vote vote = this.plugin.getVoteByID(arguments[0]);

        if (vote == null || vote != null && !this.hilda.getBot().getTextChannelById(vote.getChannelId()).equals(message.getChannel())) {
            this.reply(message, "I couldn't find that vote.");
            return;
        }

        if (vote.getOpenerId() != member.getUser().getDiscriminator() || !member.hasPermission(message.getTextChannel(), Permission.MANAGE_CHANNEL)) {
            this.reply(message, "You don't have permission to use that command.");
            return;
        }

        this.plugin.remove(vote);
        this.reply(message, "Vote deleted.");
    }

}
