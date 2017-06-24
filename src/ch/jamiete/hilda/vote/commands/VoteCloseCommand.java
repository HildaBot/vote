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
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.vote.Vote;
import ch.jamiete.hilda.vote.VotePlugin;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

public class VoteCloseCommand extends ChannelSubCommand {
    private final VotePlugin plugin;

    protected VoteCloseCommand(final Hilda hilda, final ChannelSeniorCommand senior, final VotePlugin plugin) {
        super(hilda, senior);

        this.plugin = plugin;

        this.setName("close");
        this.setDescription("Closes a vote.");
        this.setAliases(Arrays.asList(new String[] { "end", "stop" }));
        this.setMinimumPermission(Permission.MANAGE_CHANNEL);
    }

    @Override
    public void execute(final Message message, final String[] arguments, final String label) {
        if (arguments.length != 1) {
            this.usage(message, "<id>", label);
            return;
        }

        final Vote vote = this.plugin.getVoteByID(arguments[0]);

        if (vote == null) {
            this.reply(message, "I couldn't find that vote.");
            return;
        }

        vote.finish();
    }

}
