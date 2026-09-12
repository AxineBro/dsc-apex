package com.hackathon.agent.application.dto;

import com.hackathon.agent.api.dto.response.ChatAttachment;
import com.hackathon.agent.domain.model.SessionState;

import java.util.List;

public record ProcessResult(
        String reply,
        SessionState state,
        boolean transferredToManager,
        String managerReason,
        List<ChatAttachment> attachments
) {}
