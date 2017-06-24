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

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.commands.CommandManager;
import ch.jamiete.hilda.vote.Vote;
import ch.jamiete.hilda.vote.VotePlugin;
import ch.jamiete.hilda.vote.VoteTimer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class VoteStartCommand extends ChannelSubCommand {
    private final VotePlugin plugin;

    public VoteStartCommand(final Hilda hilda, final ChannelSeniorCommand senior, final VotePlugin plugin) {
        super(hilda, senior);

        this.plugin = plugin;

        this.setName("start");
        this.setDescription("Creates a vote.");
        this.setAliases(Arrays.asList(new String[] { "create", "new" }));
    }

    @Override
    public void execute(final Message message, final String[] arguments, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        final Vote vote = new Vote(this.hilda, this.plugin);
        vote.setId(this.plugin.getFreshID());
        vote.setOpener(member.getEffectiveName());
        vote.setOpenerId(member.getUser().getDiscriminator());
        vote.setAvatar(member.getUser().getAvatarUrl());
        vote.setChannelId(message.getTextChannel().getId());
        vote.setCommencement(System.currentTimeMillis());

        final ScheduledFuture<?> future = this.hilda.getExecutor().schedule(new VoteTimer(vote), VotePlugin.MAXIMUM_LENGTH, TimeUnit.MILLISECONDS);
        vote.setFuture(future);

        vote.setQuestion(Util.combineSplit(0, arguments, " ").trim());

        if (vote.getQuestion().length() == 0 || StringUtils.isNumeric(arguments[0]) && vote.getQuestion().replace(arguments[0], "").trim().equals("")) {
            this.usage(message, "<question>", label);
            return;
        }

        if (StringUtils.isNumeric(arguments[0])) {
            vote.setQuestion(Util.combineSplit(1, arguments, " ").trim());
            vote.setPercent(Integer.parseInt(arguments[0]));
        }

        this.plugin.addVote(vote);

        final EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(StringUtils.abbreviate(vote.getQuestion(), 256), null);
        eb.setColor(Color.decode("#eeb200"));

        if (vote.getPercent() == null) {
            eb.setFooter("Vote " + vote.getId() + " opened by " + vote.getOpener(), vote.getAvatar());
        } else {
            eb.setFooter("Vote " + vote.getId() + " opened by " + vote.getOpener() + " with supermajority requirement of " + vote.getPercent() + " per cent", vote.getAvatar());
        }

        eb.addField("Agree?", CommandManager.PREFIX + "v register " + vote.getId() + " yea", false);
        eb.addField("Disgree?", CommandManager.PREFIX + "v register " + vote.getId() + " nay", false);
        eb.addField("Wish to abstain?", CommandManager.PREFIX + "v register " + vote.getId() + " abstain", false);

        this.reply(message, eb.build());

        if (message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
            message.delete().queue();
        }
    }

}
