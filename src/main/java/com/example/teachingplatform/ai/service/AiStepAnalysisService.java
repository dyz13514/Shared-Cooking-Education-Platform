package com.example.teachingplatform.ai.service;

import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.task.model.LearningTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiStepAnalysisService {

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    private final AiAssistantService aiAssistantService;
    private final AiVisionService aiVisionService;

    public AiStepAnalysisService(AiAssistantService aiAssistantService,
                                 AiVisionService aiVisionService) {
        this.aiAssistantService = aiAssistantService;
        this.aiVisionService = aiVisionService;
    }

    public AnalysisResult analyze(LearningTask task, String question, MultipartFile image) {
        String safeQuestion = question == null || question.isBlank() ? "学生未提出具体问题，请结合当前画面给出步骤建议。" : question.trim();
        String title = task.getTitle() == null ? "当前任务" : task.getTitle();
        String requirement = task.getRequirements() == null ? "暂无明确要求" : task.getRequirements();
        AnalysisResult fallback = localFallback(title, requirement, safeQuestion, image != null && !image.isEmpty() ? "已收到当前操作截图" : "未收到截图");
        if (!aiEnabled) {
            return fallback;
        }
        String suggestion = image != null && !image.isEmpty()
                ? aiVisionService.analyzeImage(buildTaskVisionPrompt(task, safeQuestion), image, fallback.suggestion())
                : aiAssistantService.replyForTaskAnalysis(task, safeQuestion, false, fallback.suggestion());
        return mergeSuggestion(fallback, suggestion);
    }

    public AnalysisResult analyze(CourseStep step, String question, MultipartFile image) {
        String safeQuestion = question == null || question.isBlank() ? "学生未提出具体问题，请结合当前步骤画面给出建议。" : question.trim();
        String title = step.getTitle() == null ? "当前步骤" : step.getTitle();
        String requirement = step.getDescription() == null ? "暂无步骤说明" : step.getDescription();
        AnalysisResult fallback = localFallback(title, requirement, safeQuestion, image != null && !image.isEmpty() ? "已收到当前操作截图" : "未收到截图");
        if (!aiEnabled) {
            return fallback;
        }
        String suggestion = image != null && !image.isEmpty()
                ? aiVisionService.analyzeImage(buildStepVisionPrompt(step, safeQuestion), image, fallback.suggestion())
                : aiAssistantService.replyForStepAnalysis(step, safeQuestion, false, fallback.suggestion());
        return mergeSuggestion(fallback, suggestion);
    }

    private AnalysisResult mergeSuggestion(AnalysisResult fallback, String suggestion) {
        return new AnalysisResult(
                fallback.stageMatch(),
                0,
                clean(fallback.summary() + "。已结合上传画面生成个性化建议。"),
                fallback.problem(),
                clean(suggestion),
                fallback.nextAction(),
                fallback.risk(),
                fallback.context()
        );
    }

    private String buildTaskVisionPrompt(LearningTask task, String question) {
        return "请你作为烹饪实践课多模态助教，重点观察学生上传的操作图片，并结合任务上下文给出中文反馈。"
                + "\n任务标题：" + safe(task.getTitle())
                + "\n任务要求：" + safe(task.getRequirements())
                + "\n任务说明：" + safe(task.getDescription())
                + "\n用户个性化问题：" + safe(question)
                + "\n回答要求：先直接回应用户问题；然后必须更多结合图片可见信息进行分析，例如画面中能看到的食材状态、颜色、形态、锅具或容器、摆放、火候迹象、湿润度、成熟度、是否结块、是否焦糊、操作台整洁度和安全风险。"
                + "\n如果图片信息不足，要明确说明哪些地方看不清，再给出基于任务要求的判断。"
                + "\n最后给出具体下一步操作建议和提交记录写法。不要输出 Markdown，不要出现 * 或 #。";
    }

    private String buildStepVisionPrompt(CourseStep step, String question) {
        return "请你作为烹饪实践课多模态助教，重点观察学生上传的操作图片，并结合当前步骤给出中文反馈。"
                + "\n步骤标题：" + safe(step.getTitle())
                + "\n步骤说明：" + safe(step.getDescription())
                + "\n用户个性化问题：" + safe(question)
                + "\n回答要求：先直接回应用户问题；然后必须更多结合图片可见信息进行分析，例如画面中能看到的食材状态、颜色、形态、锅具或容器、摆放、火候迹象、湿润度、成熟度、是否结块、是否焦糊、操作台整洁度和安全风险。"
                + "\n如果图片信息不足，要明确说明哪些地方看不清，再给出基于当前步骤的判断。"
                + "\n最后给出具体下一步操作建议和步骤记录写法。不要输出 Markdown，不要出现 * 或 #。";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "无" : value.trim();
    }

    private AnalysisResult localFallback(String title, String requirement, String question, String imageState) {
        String problem = "当前为轻量演示分析，建议重点检查是否紧扣任务步骤、画面记录是否清楚。";
        String suggestion = "请对照任务要求补充关键过程、操作要点和反思。";
        String nextAction = "完成当前步骤后，继续上传图片/视频并在文本心得中说明改进点。";
        String risk = "注意镜头要对准操作台，避免只拍到成品而缺少过程。";

        if (contains(question, "结块", "米饭")) {
            problem = "米饭可能存在结块，翻炒和压散过程还需要更充分。";
            suggestion = "先用锅铲轻压大块米饭，再保持中火快速翻炒。";
            nextAction = "等米粒分散后再统一调味，并补拍一张过程图。";
        } else if (contains(question, "火", "糊", "焦")) {
            problem = "可能存在火力偏大或停留时间过长的问题。";
            suggestion = "适当降低火力，保持持续翻动，避免局部受热过久。";
            nextAction = "先调整火候，再继续完成当前步骤。";
        } else if (contains(question, "蛋", "老", "嫩")) {
            problem = "蛋液可能加热过久，嫩度和成型速度需要控制。";
            suggestion = "蛋液七成熟即可盛出或回锅，避免持续高温翻炒。";
            nextAction = "记录本次火候问题，并在反思里写出下次改进。";
        }

        return new AnalysisResult(
                "围绕当前任务分析",
                0.62,
                imageState + "。任务背景：" + shorten(title, 30),
                problem,
                suggestion,
                nextAction,
                risk,
                shorten(requirement, 80)
        );
    }

    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("*", "").replace("#", "").trim();
    }

    private boolean contains(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        String trimmed = text.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    public record AnalysisResult(
            String stageMatch,
            double confidence,
            String summary,
            String problem,
            String suggestion,
            String nextAction,
            String risk,
            String context
    ) {}
}
