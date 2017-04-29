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

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import org.apache.commons.lang3.StringUtils;
import ch.jamiete.hilda.Hilda;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class Vote implements Serializable {
    private static final long serialVersionUID = 1L;

    transient Hilda hilda;
    transient VotePlugin plugin;
    private transient ScheduledFuture<?> future;

    private String channel_id;
    private String opener;
    private String opener_id;
    private String avatar;
    private String id;
    private String question;

    private Integer percent;

    private Long commencement = Long.MIN_VALUE;

    private final HashMap<String, VoteResponse> responses = new HashMap<String, VoteResponse>();

    /**
     * Instantiates an empty Vote. <b>Only use this where votes are being loaded from disk.</b>
     */
    public Vote() {

    }

    public Vote(final Hilda hilda, final VotePlugin plugin) {
        this.hilda = hilda;
        this.plugin = plugin;
    }

    /**
     * Ends the vote if all members of the channel have voted on it.
     */
    public void check() {
        for (final Member member : this.hilda.getBot().getTextChannelById(this.channel_id).getMembers()) {
            if (!this.hasVoted(member) && !member.getUser().isBot()) {
                return;
            }
        }

        this.finish();
    }

    /**
     * Ends the vote.
     */
    public void finish() {
        final TextChannel channel = this.hilda.getBot().getTextChannelById(this.channel_id);
        channel.sendTyping();

        final EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(StringUtils.abbreviate(this.question, 256), null);

        if (this.percent == null) {
            eb.setFooter("Vote " + this.id + " opened by " + this.opener, this.avatar);
        } else {
            eb.setFooter("Vote " + this.id + " opened by " + this.opener + " with supermajority requirement of " + this.percent + " per cent", this.avatar);
        }

        final ArrayList<Member> yea = new ArrayList<Member>();
        final ArrayList<Member> nay = new ArrayList<Member>();
        final ArrayList<Member> abstain = new ArrayList<Member>();

        for (final Entry<String, VoteResponse> entry : this.responses.entrySet()) {
            final Member member = channel.getGuild().getMemberById(entry.getKey());

            if (member == null) {
                break;
            }

            switch (entry.getValue()) {
                case YEA:
                    yea.add(member);
                    break;

                case NAY:
                    nay.add(member);
                    break;

                case ABSTAIN:
                    abstain.add(member);
                    break;
            }
        }

        final StringBuilder yeas = new StringBuilder();
        if (yea.size() > 0) {
            for (final Member member : yea) {
                yeas.append(member.getEffectiveName()).append(", ");
            }

            yeas.setLength(yeas.length() - 2);
        } else {
            yeas.append("None");
        }
        eb.addField("YEA (" + yea.size() + ")", yeas.toString(), true);

        final StringBuilder nays = new StringBuilder();
        if (nay.size() > 0) {
            for (final Member member : nay) {
                nays.append(member.getEffectiveName()).append(", ");
            }

            nays.setLength(nays.length() - 2);
        } else {
            nays.append("None");
        }
        eb.addField("NAY (" + nay.size() + ")", nays.toString(), true);

        for (final Member member : channel.getMembers()) {
            if (this.hasVoted(member) || member.getUser().isBot()) {
                continue;
            }

            abstain.add(member);
        }

        if (abstain.size() <= 15) {
            final StringBuilder abstentions = new StringBuilder();
            if (abstain.size() > 0) {
                for (final Member member : abstain) {
                    abstentions.append(member.getEffectiveName()).append(", ");
                }

                abstentions.setLength(abstentions.length() - 2);
            } else {
                abstentions.append("None");
            }
            eb.addField("ABSTENTIONS (" + abstain.size() + ")", abstentions.toString(), true);
        }

        // Counting logic
        if (this.percent == null) {
            if (yea.size() > nay.size()) {
                eb.addField("RESULT", "The yeas have it.", false);
                eb.setColor(Color.decode("#32b67a"));
            } else if (nay.size() > yea.size()) {
                eb.addField("RESULT", "The nays have it.", false);
                eb.setColor(Color.decode("#e54b4b"));
            } else if (yea.size() == 0 && nay.size() == 0) {
                eb.addField("RESULT", "The vote failed as everyone abstained.", false);
                eb.setColor(Color.decode("#c0c2ce"));
            } else {
                eb.addField("RESULT", "The vote tied.", false);
                eb.setColor(Color.decode("#c0c2ce"));
            }
        } else {
            final int sum = yea.size() + nay.size();

            if (sum == 0) {
                eb.addField("RESULT", "The vote failed as everyone abstained.", false);
                eb.setColor(Color.decode("#c0c2ce"));
            } else {
                final double yeapc = (double) yea.size() / (double) sum * 100;

                Hilda.getLogger().info("yea " + yea.size() + " nay " + nay.size() + " sum " + sum + " pc " + yeapc);

                if (yeapc >= this.percent) {
                    eb.addField("RESULT", "The yeas have it.", false);
                    eb.setColor(Color.decode("#32b67a"));
                } else if (yeapc == 50.00) {
                    eb.addField("RESULT", "The vote tied.", false);
                    eb.setColor(Color.decode("#c0c2ce"));
                } else {
                    eb.addField("RESULT", "The yeas (" + Math.round(yeapc) + "%) did not reach the required supermajority of " + this.percent + "%.", false);
                    eb.setColor(Color.decode("#e54b4b"));
                }
            }
        }

        channel.sendMessage(eb.build()).queue();

        this.plugin.remove(this);

        if (this.getFuture() != null) {
            this.getFuture().cancel(false);
        }
    }

    public String getAvatar() {
        return this.avatar;
    }

    public String getChannelId() {
        return this.channel_id;
    }

    public Long getCommencement() {
        return this.commencement;
    }

    public ScheduledFuture<?> getFuture() {
        return this.future;
    }

    public String getId() {
        return this.id;
    }

    public String getOpener() {
        return this.opener;
    }

    public String getOpenerId() {
        return this.opener_id;
    }

    public Integer getPercent() {
        return this.percent;
    }

    public String getQuestion() {
        return this.question;
    }

    public HashMap<String, VoteResponse> getResponses() {
        return this.responses;
    }

    public boolean hasVoted(final Member member) {
        return this.responses.containsKey(member.getUser().getId());
    }

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    public void setChannelId(final String channel_id) {
        this.channel_id = channel_id;
    }

    public void setCommencement(final long commencement) {
        this.commencement = commencement;
    }

    public void setFuture(final ScheduledFuture<?> future) {
        this.future = future;
    }

    public void setHilda(final Hilda hilda) {
        if (this.hilda == null) {
            this.hilda = hilda;
        }
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setOpener(final String opener) {
        this.opener = opener;
    }

    public void setOpenerId(final String opener_id) {
        this.opener_id = opener_id;
    }

    public void setPercent(final Integer percent) {
        this.percent = percent;
    }

    public void setPlugin(final VotePlugin plugin) {
        if (this.plugin == null) {
            this.plugin = plugin;
        }
    }

    public void setQuestion(final String question) {
        this.question = question;
    }
}