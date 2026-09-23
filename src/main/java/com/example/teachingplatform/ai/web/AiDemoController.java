package com.example.teachingplatform.ai.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiDemoController {

    @GetMapping(value = "/suggestion-demo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> suggestionDemo() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "mock");
        data.put("title", "AI 辅助建议 Demo");
        data.put("summary", "这里是 AI 接口占位示例，后续可替换为你的真实 API Key 与模型调用。");
        data.put("highlights", List.of(
                "提示学生先完成步骤一再进入步骤二",
                "建议教师关注火候与节奏控制",
                "保留人工确认后再下发给学生"
        ));
        return data;
    }
}
