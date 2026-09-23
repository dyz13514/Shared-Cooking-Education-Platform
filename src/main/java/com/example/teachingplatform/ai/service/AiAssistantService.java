package com.example.teachingplatform.ai.service;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.task.model.LearningTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAssistantService {

    private final BigModelChatService bigModelChatService;

    public AiAssistantService(BigModelChatService bigModelChatService) {
        this.bigModelChatService = bigModelChatService;
    }

    public String replyForMessage(Role role, String content) {
        return replyForMessage(role, content, List.of());
    }

    public String replyForMessage(Role role, String content, List<BigModelChatService.Message> history) {
        String fallback = fallbackReply(content);
        String systemPrompt = systemPromptFor(role);
        return cleanReply(bigModelChatService.chat(systemPrompt,
                withCurrentUserMessage(history, content),
                fallback));
    }

    public String streamReplyForMessage(Role role, String content, java.util.function.Consumer<String> onDelta) {
        return streamReplyForMessage(role, content, List.of(), onDelta);
    }

    public String streamReplyForMessage(Role role, String content, List<BigModelChatService.Message> history, java.util.function.Consumer<String> onDelta) {
        String fallback = fallbackReply(content);
        String systemPrompt = systemPromptFor(role);
        StringBuilder pending = new StringBuilder();
        String fullReply = bigModelChatService.streamChat(systemPrompt,
                withCurrentUserMessage(history, content),
                delta -> {
                    String cleaned = cleanDelta(delta, pending);
                    if (!cleaned.isEmpty()) {
                        onDelta.accept(cleaned);
                    }
                },
                fallback);
        String tail = cleanReply(pending.toString());
        if (!tail.isBlank()) {
            onDelta.accept(tail);
        }
        return cleanReply(fullReply);
    }

    public String replyForTaskAnalysis(LearningTask task, String question, boolean hasImage, String fallback) {
        String prompt = "请你作为烹饪实践课智能助手，基于以下任务生成个性化反馈。"
                + "\n任务标题：" + safe(task.getTitle())
                + "\n任务要求：" + safe(task.getRequirements())
                + "\n任务说明：" + safe(task.getDescription())
                + "\n用户个性化问题：" + safe(question)
                + "\n是否提供截图：" + (hasImage ? "是" : "否")
                + "\n回答要求：先直接回应用户问题，再结合任务目标给出具体建议，可适度展开原因、判断依据、下一步操作和反思写法。不要输出 Markdown，不要出现 * 或 #。";
        return cleanReply(bigModelChatService.chat("你是厨学实践课智能助手。用自然中文回答，不使用 Markdown 符号，不出现 * 和 #。", List.of(new BigModelChatService.Message("user", prompt)), fallback));
    }

    public String replyForStepAnalysis(CourseStep step, String question, boolean hasImage, String fallback) {
        String prompt = "请你作为烹饪实践课智能助手，基于以下课时步骤生成个性化反馈。"
                + "\n步骤标题：" + safe(step.getTitle())
                + "\n步骤说明：" + safe(step.getDescription())
                + "\n用户个性化问题：" + safe(question)
                + "\n是否提供截图：" + (hasImage ? "是" : "否")
                + "\n回答要求：先直接回应用户问题，再结合当前步骤给出具体建议，可适度展开原因、判断依据、下一步操作和反思写法。不要输出 Markdown，不要出现 * 或 #。";
        return cleanReply(bigModelChatService.chat("你是厨学实践课智能助手。用自然中文回答，不使用 Markdown 符号，不出现 * 和 #。", List.of(new BigModelChatService.Message("user", prompt)), fallback));
    }

    private String systemPromptFor(Role role) {
        String common = "回答要求：优先直接回答用户当前问题，不要一上来就要求用户进入课程、任务或资源页面；"
                + "如果问题是常识、烹饪技巧、学习方法、教学设计、作业写法或平台使用咨询，先给出可执行的直接答案；"
                + "回答完后，如确实有帮助，再用一两句话结合平台功能补充建议，例如可以查看相关课程、课时步骤、任务要求、资源或提交记录；"
                + "只有当缺少必要信息导致无法判断时，才追问课程名、任务要求、步骤内容等关键信息；"
                + "不要把用户称为学生，直接用“你”；语气自然灵活，不要模板化；"
                + "使用简洁自然的中文，不输出 Markdown 符号，不使用 *、#、加粗标题或井号标题。";
        if (role == Role.STUDENT) {
            return "你是厨学实践教学平台里的智能助手。背景：这是面向学生的烹饪实践教学平台，包含课程、课时、任务、步骤、提交和资源。"
                    + "你的职责是先帮助用户理解和解决实际问题，包括烹饪操作、学习进度、任务要求、步骤顺序、提交规范、实践改进和反思写法；"
                    + "平台相关内容只作为回答后的辅助推荐，不要替代直接回答。" + common;
        }
        return "你是厨学实践教学平台里的教师端智能助手。背景：这是面向教师的烹饪实践教学平台，包含课程建设、课时步骤、任务发布、作业批改、资源管理和教学组织。"
                + "你的职责是先帮助用户解决备课、任务设计、批改反馈、课堂组织、资源管理和学生学习情况分析等实际问题；"
                + "平台相关内容只作为回答后的辅助推荐，不要替代直接回答。" + common;
    }

    private List<BigModelChatService.Message> withCurrentUserMessage(List<BigModelChatService.Message> history, String content) {
        java.util.ArrayList<BigModelChatService.Message> messages = new java.util.ArrayList<>();
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(new BigModelChatService.Message("user", content));
        return messages;
    }

    private String cleanDelta(String delta, StringBuilder pending) {
        if (delta == null || delta.isEmpty()) {
            return "";
        }
        pending.append(delta);
        String current = pending.toString();
        String cleaned = cleanReply(current);
        pending.setLength(0);
        return cleaned;
    }

    private String cleanReply(String reply) {
        if (reply == null) {
            return "";
        }
        return reply.replace("*", "")
                .replace("#", "")
                .replace("智能教师", "智能助手")
                .replace("AI 教学助手", "智能助手")
                .replace("AI 助教", "智能助手")
                .trim();
    }

    private String fallbackReply(String content) {
        if (content == null || content.isBlank()) {
            return "我在。你可以直接问烹饪操作、学习任务、作业提交、课程设计或平台使用问题，我会先给出具体建议。";
        }
        String lower = content.toLowerCase();
        if (lower.contains("任务") || lower.contains("作业")) {
            return "可以先把任务拆成目标、操作步骤、提交材料和评价标准四部分。先确认要完成什么，再列出过程记录、成品图片或反思说明等提交内容；如果是在平台里操作，再查看对应任务的要求和截止时间。";
        }
        if (lower.contains("课时") || lower.contains("步骤") || lower.contains("顺序")) {
            return "一般可以按准备、示范、练习、纠错、总结的顺序安排。每个步骤最好只突出一个核心操作点，并配一个常见错误提醒；如果平台里已有课时步骤，可以再对照步骤说明逐项检查。";
        }
        if (lower.contains("蛋炒饭") || lower.contains("炒饭")) {
            return "蛋炒饭的关键是米饭要偏干松散，先把蛋炒到半凝固，再下米饭快速打散，最后调味并用中大火翻炒出香气。评价时重点看米粒是否分明、蛋香是否均匀、咸淡是否合适、成品是否油而不腻；如果要提交作业，可以补充过程照片和火候控制反思。";
        }
        if (lower.contains("提交") || lower.contains("上传")) {
            return "提交前先检查材料是否完整：过程记录、成品图片、简短说明或反思。文字反思可以写自己做得好的地方、出现的问题、原因分析和下次改进；如果是在平台提交，再确认对应任务或步骤的上传入口和要求。";
        }
        return "我可以先按你的问题给出直接建议：先明确目标，再判断当前卡点属于操作方法、任务理解、材料准备、时间安排还是提交规范。你也可以继续补充具体场景，我会直接帮你分析；如果和平台课程或任务有关，我再顺带告诉你应该查看哪里。";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "无" : value.trim();
    }
}
