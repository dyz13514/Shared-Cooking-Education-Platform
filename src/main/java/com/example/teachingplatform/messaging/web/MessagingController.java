package com.example.teachingplatform.messaging.web;

import com.example.teachingplatform.ai.service.AiAssistantService;
import com.example.teachingplatform.ai.service.BigModelChatService;
import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.messaging.model.TeacherMessage;
import com.example.teachingplatform.messaging.service.TeacherMessageService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/messages")
public class MessagingController {

    private final TeacherMessageService messageService;
    private final UserRepository userRepository;
    private final AiAssistantService aiAssistantService;

    public MessagingController(TeacherMessageService messageService,
                               UserRepository userRepository,
                               AiAssistantService aiAssistantService) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping
    public String index(@RequestParam(name = "peerId", required = false) String peerId,
                        @RequestParam(name = "q", required = false) String q,
                        Model model,
                        HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        if (uid == null || role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long aiPeerId = -1L;
        Long selectedPeerId = "ai".equalsIgnoreCase(peerId) ? aiPeerId : parseLong(peerId);
        List<User> peers = role == Role.STUDENT
                ? userRepository.findAll().stream().filter(u -> u.getRole() == Role.TEACHER).toList()
                : userRepository.findAll().stream().filter(u -> u.getRole() == Role.STUDENT).toList();
        if (q != null && !q.isBlank()) {
            String keyword = q.toLowerCase();
            peers = peers.stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().contains(keyword))
                    .toList();
        }
        Long teacherId = selectedPeerId != null && selectedPeerId.equals(aiPeerId) ? aiPeerId : (role == Role.STUDENT ? selectedPeerId : uid);
        Long studentId = selectedPeerId != null && selectedPeerId.equals(aiPeerId) ? uid : (role == Role.STUDENT ? uid : selectedPeerId);
        List<TeacherMessage> messages = selectedPeerId == null ? List.of() : messageService.listThread(teacherId, studentId);
        model.addAttribute("pageTitle", "私信老师");
        model.addAttribute("mode", role.name().toLowerCase());
        model.addAttribute("peers", peers);
        model.addAttribute("peerId", peerId);
        model.addAttribute("q", q);
        model.addAttribute("messages", messages);
        boolean aiMode = selectedPeerId != null && selectedPeerId.equals(aiPeerId);
        model.addAttribute("peerName", resolvePeerName(selectedPeerId, peers));
        model.addAttribute("selfLabel", "我");
        model.addAttribute("otherLabel", aiMode ? "智能助手" : (role == Role.STUDENT ? "老师" : "学生"));
        model.addAttribute("aiMode", aiMode);
        MessageForm form = new MessageForm();
        form.setTargetId(selectedPeerId);
        model.addAttribute("form", form);
        return "messages/index";
    }

    @PostMapping
    public String send(@ModelAttribute("form") MessageForm form, HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        if (uid == null || role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (form.getTargetId() == null) {
            return "redirect:/messages";
        }
        Long aiPeerId = -1L;
        if (form.getTargetId() < 0) {
            List<TeacherMessage> history = messageService.listThread(aiPeerId, uid);
            List<BigModelChatService.Message> aiHistory = toAiHistory(history);
            messageService.send(aiPeerId, uid, form.getContent(), true);
            messageService.send(aiPeerId, uid, aiAssistantService.replyForMessage(role, form.getContent(), aiHistory), false);
            return "redirect:/messages?peerId=ai";
        }
        Long teacherId = role == Role.STUDENT ? form.getTargetId() : uid;
        Long studentId = role == Role.STUDENT ? uid : form.getTargetId();
        messageService.send(teacherId, studentId, form.getContent(), role == Role.STUDENT);
        return "redirect:/messages?peerId=" + form.getTargetId();
    }

    @PostMapping(value = "/ai/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public StreamingResponseBody sendAi(@Valid @RequestBody AiSendRequest request,
                                        HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        if (uid == null || role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long aiPeerId = -1L;
        List<TeacherMessage> history = messageService.listThread(aiPeerId, uid);
        List<BigModelChatService.Message> aiHistory = toAiHistory(history);
        TeacherMessage userMessage = messageService.send(aiPeerId, uid, request.content(), true);
        return outputStream -> {
            writeSseEvent(outputStream, "user", userMessage.getId(), userMessage.getContent());
            StringBuilder reply = new StringBuilder();
            String fullReply = aiAssistantService.streamReplyForMessage(role, request.content(), aiHistory, delta -> {
                if (delta == null || delta.isBlank()) {
                    return;
                }
                reply.append(delta);
                try {
                    writeSseEvent(outputStream, "delta", null, delta);
                } catch (Exception ignored) {
                    // client disconnected
                }
            });
            if (reply.isEmpty() && fullReply != null && !fullReply.isBlank()) {
                reply.append(fullReply);
                writeSseEvent(outputStream, "delta", null, fullReply);
            }
            TeacherMessage aiMessage = messageService.send(aiPeerId, uid, reply.toString(), false);
            writeSseEvent(outputStream, "done", aiMessage.getId(), "");
        };
    }

    @PostMapping("/ai/messages/{messageId}/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteAiMessage(@PathVariable Long messageId,
                                                HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        if (uid == null || role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        messageService.delete(messageId, -1L, uid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("messageId") Long messageId,
                         @RequestParam("peerId") String peerId,
                         HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        if (uid == null || role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long parsedPeerId = parseLong(peerId);
        if (parsedPeerId == null || parsedPeerId < 0) {
            return "redirect:/messages?peerId=ai";
        }
        Long teacherId = role == Role.STUDENT ? parsedPeerId : uid;
        Long studentId = role == Role.STUDENT ? uid : parsedPeerId;
        messageService.delete(messageId, teacherId, studentId);
        return "redirect:/messages?peerId=" + parsedPeerId;
    }

    private List<BigModelChatService.Message> toAiHistory(List<TeacherMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, messages.size() - 10);
        return messages.subList(fromIndex, messages.size()).stream()
                .map(message -> new BigModelChatService.Message(message.isFromStudent() ? "user" : "assistant", message.getContent()))
                .toList();
    }

    private void writeSseEvent(java.io.OutputStream outputStream, String type, Long id, String content) throws java.io.IOException {
        String json = "{\"type\":" + json(type)
                + ",\"id\":" + (id == null ? "null" : id)
                + ",\"content\":" + json(content)
                + "}";
        String event = "data: " + json + "\n\n";
        outputStream.write(event.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String json(String value) {
        if (value == null) return "\"\"";
        StringBuilder sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private String resolvePeerName(Long peerId, List<User> peers) {
        if (peerId == null) {
            return null;
        }
        if (peerId < 0) {
            return "智能助手";
        }
        return peers.stream().filter(u -> peerId.equals(u.getId()))
                .findFirst()
                .map(u -> u.getRealName() != null && !u.getRealName().isEmpty() ? u.getRealName() : u.getUsername())
                .orElse(null);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Getter
    @Setter
    public static class MessageForm {
        @NotNull
        private Long targetId;
        @NotBlank
        private String content;
    }

    public record AiSendRequest(@NotBlank String content) {}

    public record MessageView(Long id, String content, boolean fromStudent) {}

    public record AiSendResponse(MessageView userMessage, MessageView aiMessage) {}
}
