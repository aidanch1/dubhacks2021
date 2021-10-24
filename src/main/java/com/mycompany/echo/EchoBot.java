// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.mycompany.echo;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.ChannelAccount;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.bot.ai.qna.QnAMaker;
import com.microsoft.bot.ai.qna.QnAMakerEndpoint;
import com.microsoft.bot.integration.Configuration;

/**
 * This class implements the functionality of the Bot.
 *
 * <p>
 * This is where application specific logic for interacting with the users would be added. For this
 * sample, the {@link #onMessageActivity(TurnContext)} echos the text back to the user. The {@link
 * #onMembersAdded(List, TurnContext)} will send a greeting to new conversation participants.
 * </p>
 */
public class EchoBot extends ActivityHandler {

    QnAMaker qnaMaker;

    public EchoBot(Configuration configuration) {
        QnAMakerEndpoint qnAMakerEndpoint = new QnAMakerEndpoint();
        qnAMakerEndpoint.setKnowledgeBaseId(configuration.getProperty("QnAKnowledgebaseId"));
        qnAMakerEndpoint.setEndpointKey(configuration.getProperty("QnAEndpointKey"));
        qnAMakerEndpoint.setHost(configuration.getProperty("QnAEndpointHostName"));

        qnaMaker = new QnAMaker(qnAMakerEndpoint, null);

    }

    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        return turnContext.sendActivity(MessageFactory.text("Echo: " + turnContext.getActivity().getText()))
            .thenCompose(sendResult -> {
                return accessQnAMaker(turnContext);
            });
    }

    @Override
    protected CompletableFuture<Void> onMembersAdded(
        List<ChannelAccount> membersAdded,
        TurnContext turnContext
    ) {
        return membersAdded.stream()
            .filter(
                member -> !StringUtils
                    .equals(member.getId(), turnContext.getActivity().getRecipient().getId())
            ).map(channel -> turnContext.sendActivity(MessageFactory.text("HAHA")))
            .collect(CompletableFutures.toFutureList()).thenApply(resourceResponses -> null);
    }

    private CompletableFuture<Void> accessQnAMaker(TurnContext turnContext) {
        return qnaMaker.getAnswers(turnContext, null).thenCompose(results -> {
            if (results.length > 0) {
                return turnContext
                    .sendActivity(MessageFactory.text(String.format("QnA Maker Returned: %s" + results[0].getAnswer())))
                    .thenApply(result -> null);
            } else {
                return turnContext
                    .sendActivity(MessageFactory.text("Sorry, could not find an answer in the knowledge base."))
                    .thenApply(result -> null);
            }
        });
    }
}
